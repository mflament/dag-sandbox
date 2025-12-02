package org.yah.test.marshall;

import org.yah.test.marshall.NativeInstancesLayout.LayoutEntry;
import org.yah.test.marshall.NativeObject.NativeField;
import org.yah.test.marshall.bytebuffer.MappedByteBufferAllocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.yah.test.marshall.NativeObjectFileMarshaller.LAYOUT_ENTRY_SIZE;

public class NativeObjectFileUnmarshaller {

    public static final class NativeObjectsFile implements AutoCloseable {
        private final NativeInstances<MappedByteBufferAllocation> nativeInstances;
        private final NativeObjectUnmarshaller unmarshaller;

        public NativeObjectsFile(NativeInstances<MappedByteBufferAllocation> nativeInstances, NativeObjectUnmarshaller unmarshaller) {
            this.nativeInstances = nativeInstances;
            this.unmarshaller = unmarshaller;
        }

        public Object unmarshall() {
            return unmarshaller.unmarshall(nativeInstances);
        }

        @SuppressWarnings("unchecked")
        public <T> T unmarshall(Class<T> type) {
            Object unmarshalled = unmarshaller.unmarshall(nativeInstances);
            if (type.isInstance(unmarshalled))
                return (T) unmarshalled;
            throw new IllegalStateException("Unmarshalled instance " + unmarshalled.getClass().getTypeName() + " is not an instance of " + type.getTypeName());
        }

        public void unmarshall(Object instance) throws IOException {
            unmarshaller.unmarshall(nativeInstances, instance);
        }

        @Override
        public void close() {
            nativeInstances.allocation().close();
        }
    }

    @FunctionalInterface
    public interface InstanceFactory {
        @Nullable
        Object create(Class<?> type);
    }

    private final TypeIntrospector typeIntrospector;
    private final @Nullable InstanceFactory instanceFactory;

    public NativeObjectFileUnmarshaller() {
        this(null, null);
    }

    public NativeObjectFileUnmarshaller(@Nullable TypeIntrospector typeIntrospector) {
        this(typeIntrospector, null);
    }

    public NativeObjectFileUnmarshaller(@Nullable TypeIntrospector typeIntrospector, @Nullable InstanceFactory instanceFactory) {
        this.typeIntrospector = new CachingTypeIntrospector(Objects.requireNonNullElse(typeIntrospector, DefaultTypeIntrospector.INSTANCE));
        this.instanceFactory = instanceFactory;
    }

    public NativeObjectsFile loadNativeObjectsFile(Path path) throws IOException {
        FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ);

        readHeader(fileChannel);

        NativeObjectsRegistry objectsRegistry = readTypes(fileChannel);
        NativeInstancesLayout layout = readLayout(fileChannel, objectsRegistry);

        // skip size
        fileChannel.position(fileChannel.position() + Long.BYTES);

        NativeObjectUnmarshaller unmarshaller = new NativeObjectUnmarshaller(objectsRegistry);
        MappedByteBufferAllocation allocation = new MappedByteBufferAllocation(fileChannel, fileChannel.position(), FileChannel.MapMode.READ_ONLY);
        NativeInstances<MappedByteBufferAllocation> nativeInstances = new NativeInstances<>(layout, allocation);
        return new NativeObjectsFile(nativeInstances, unmarshaller);
    }

    private void readHeader(FileChannel fileChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        readFully(fileChannel, buffer);
        if (!Arrays.equals(buffer.array(), NativeObjectFileMarshaller.HEADER))
            throw new IOException("Invalid header");
    }

    private NativeObjectsRegistry readTypes(FileChannel fileChannel) throws IOException {
        Map<Class<?>, Integer> classTypeIds = new HashMap<>();
        ByteBuffer buffer = ByteBuffer.allocate(4 * 1024).order(ByteOrder.LITTLE_ENDIAN);
        readFully(fileChannel, buffer.limit(Integer.BYTES));
        int count = buffer.getInt();
        for (int i = 0; i < count; i++) {
            long start = fileChannel.position() + Integer.BYTES;
            readFully(fileChannel, buffer.clear().limit(3 * Integer.BYTES));
            int entrySize = buffer.getInt();
            int typeId = buffer.getInt();
            String typeName = getString(fileChannel, buffer, buffer.getInt());
            classTypeIds.put(getClass(typeName), typeId);
            fileChannel.position(start + entrySize);
        }
        NativeObjectsRegistry registry = new NativeObjectsRegistry(typeIntrospector, classTypeIds::get);
        classTypeIds.keySet().forEach(registry::get);
        return registry;
    }

    public NativeInstancesLayout readLayout(FileChannel fileChannel, NativeObjectsRegistry objectsRegistry) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        readFully(fileChannel, buffer);
        int count = buffer.getInt();

        buffer = ByteBuffer.allocate(count * LAYOUT_ENTRY_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        readFully(fileChannel, buffer);
        NativeInstancesLayout layout = new NativeInstancesLayout(objectsRegistry);
        for (int i = 0; i < count; i++) {
            layout.addEntry(readLayoutEntry(buffer, objectsRegistry));
        }
        return layout;
    }

    private LayoutEntry readLayoutEntry(ByteBuffer buffer, NativeObjectsRegistry objectsRegistry) {
        int typeId = buffer.getInt();
        long offset = buffer.getLong();
        int size = buffer.getInt();
        int length = buffer.getInt();
        if (size < 0) {
            Class<?> componentType = getArrayComponentType(typeId, objectsRegistry);
            Object instance = Array.newInstance(componentType, length);
            if (NativeObject.isStruct(componentType)) {
                // create struct instance, it's not a reference and will not be in another LayoutEntry
                Object[] structs = ((Object[]) instance);
                for (int i = 0; i < length; i++)
                    structs[i] = createInstance(componentType, objectsRegistry);
            } else if (componentType.isEnum()) {
                // enum are values in native world, so never null. Fill with default value
                Object defaultValue = componentType.getEnumConstants()[0];
                Arrays.fill((Object[]) instance, defaultValue);
            }
            return new LayoutEntry(instance, offset, -size);
        } else {
            if (typeId < 0)
                throw new IllegalStateException("Invalid object layout entry typeId " + typeId);
            NativeObject nativeObject = objectsRegistry.get(typeId);
            Object instance = createInstance(nativeObject.type(), objectsRegistry);
            return new LayoutEntry(instance, offset, size);
        }
    }

    private static Class<?> getArrayComponentType(long typeId, NativeObjectsRegistry objectsRegistry) {
        if (typeId < 0)
            return getPrimitiveType(typeId);
        NativeObject nativeObject = objectsRegistry.get((int) typeId);
        return nativeObject.type();
    }

    private static Class<?> getPrimitiveType(long id) {
        if (id == -1) return byte.class;
        if (id == -2) return short.class;
        if (id == -3) return int.class;
        if (id == -4) return long.class;
        if (id == -5) return float.class;
        if (id == -6) return double.class;
        if (id == -7) return boolean.class;
        throw new IllegalArgumentException("Unresolved primitive type id " + id);
    }

    private Object createInstance(Class<?> type, NativeObjectsRegistry objectsRegistry) {
        Object instance = null;
        if (instanceFactory != null)
            instance = instanceFactory.create(type);
        if (instance != null)
            return instance;
        return createDefaultInstance(type, objectsRegistry);
    }

    @Nonnull
    private static Class<?> getClass(String className) {
        Class<?> type;
        try {
            type = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return type;
    }

    private static void readFully(FileChannel fileChannel, ByteBuffer byteBuffer) throws IOException {
        while (byteBuffer.hasRemaining()) {
            int read = fileChannel.read(byteBuffer);
            if (read < 0)
                throw new EOFException();
        }
        byteBuffer.flip();
    }

    private static String getString(FileChannel fileChannel, ByteBuffer buffer, int byteCount) throws IOException {
        readFully(fileChannel, buffer.clear().limit(byteCount));
        return new String(buffer.array(), 0, byteCount, StandardCharsets.UTF_8);
    }

    public Object createDefaultInstance(Class<?> type, NativeObjectsRegistry objectsRegistry) {
        Constructor<?> constructor = Arrays.stream(type.getConstructors())
                .min(Comparator.comparing(Constructor::getParameterCount))
                .orElseThrow(() -> new IllegalArgumentException("No constructor found for type " + type.getTypeName()));
        Object[] args = Arrays.stream(constructor.getParameterTypes())
                .map(pt -> defaultParameterValue(pt, objectsRegistry))
                .toArray();
        Object instance;
        try {
            instance = constructor.newInstance(args);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Error creating instance of " + type.getTypeName(), e);
        }
        NativeObject nativeObject = objectsRegistry.get(type);
        for (NativeField field : nativeObject.fields()) {
            Class<?> fieldType = field.type();
            if (NativeObject.isStruct(fieldType))
                field.varHandle().set(instance, createDefaultInstance(fieldType, objectsRegistry));
        }
        return instance;
    }

    private Object defaultParameterValue(Class<?> type, NativeObjectsRegistry objectsRegistry) {
        if (type.isPrimitive())
            return defaultPrimitiveValue(type);
        if (type.isEnum())
            return type.getEnumConstants()[0];
        if (NativeObject.isStruct(type))
            return createDefaultInstance(type, objectsRegistry);
        return null;
    }

    private static Object defaultPrimitiveValue(Class<?> type) {
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return (long) 0;
        if (type == float.class) return (float) 0;
        if (type == double.class) return (double) 0;
        if (type == boolean.class) return false;
        throw new IllegalArgumentException("Unsupported primitive type " + type.getTypeName());
    }

}
