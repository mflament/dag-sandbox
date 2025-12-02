package org.yah.test.marshall;

import org.yah.test.marshall.NativeInstancesLayout.LayoutEntry;
import org.yah.test.marshall.bytebuffer.MappedByteBufferAllocation;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class NativeObjectFileMarshaller {

    static final byte[] HEADER = "NBIN".getBytes(StandardCharsets.US_ASCII);

    public static final int LAYOUT_ENTRY_SIZE = Long.BYTES + 3 * Integer.BYTES;

    private final NativeObjectsRegistry objectsRegistry;
    private final NativeLayoutFactory layoutFactory;
    private final NativeObjectMarshaller marshaller;

    public NativeObjectFileMarshaller(NativeObjectsRegistry objectsRegistry) {
        this.objectsRegistry = Objects.requireNonNull(objectsRegistry, "objectsRegistry is null");
        layoutFactory = new NativeLayoutFactory(objectsRegistry);
        marshaller = new NativeObjectMarshaller(objectsRegistry, 1);
    }

    public void marshall(Path file, Object instance) throws IOException {
        try (FileChannel fileChannel = FileChannel.open(file, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writeHeader(fileChannel);
            NativeInstancesLayout layout = layoutFactory.createLayout(instance);
            writeTypes(layout, fileChannel);
            writeLayout(layout, fileChannel);
            writeInstances(layout, fileChannel);
        }
    }

    private void writeHeader(FileChannel fileChannel) throws IOException {
        writeFully(ByteBuffer.wrap(HEADER), fileChannel);
    }

    private void writeTypes(NativeInstancesLayout layout, FileChannel fileChannel) throws IOException {
        Set<NativeObject> referencedTypes = collectReferencedTypes(layout);
        ByteBuffer buffer = ByteBuffer.allocate(8 * 1024).order(ByteOrder.LITTLE_ENDIAN);
        writeFully(buffer.putInt(referencedTypes.size()).flip(), fileChannel);
        for (NativeObject referencedType : referencedTypes) {
            buffer.clear();
            while (true) {
                try {
                    putObjectType(referencedType, buffer);
                    break;
                } catch (BufferOverflowException e) {
                    buffer = ByteBuffer.allocate(buffer.capacity() * 2).order(ByteOrder.LITTLE_ENDIAN);
                }
            }
            writeFully(buffer.flip(), fileChannel);
        }
    }

    /**
     * int : typeId
     * string : type name
     * byte : type = 0 for Object , 1 for Struct, 2 for Enum
     * int : fields count (or enum constants count)
     * <pre>
     * if object/struct
     *      for each field
     *          string : field name
     *          int : dims (0 if not array, number of array dimensions otherwise)
     *          int : typeId (< 0 for primitive)
     * if enum
     *      for each enum constant
     *          string : constant name
     *          int : native enum value if Enum<?> & NativeEnum, ordinal otherwise
     * </pre>
     */
    private void putObjectType(NativeObject object, ByteBuffer buffer) {
        buffer.position(Integer.BYTES); // size, write at end
        buffer.putInt(object.typeId());

        Class<?> type = object.type();
        putString(type.getName(), buffer);

        if (type.isEnum()) {
            buffer.put((byte) 2);
            Enum<?>[] enumConstants = (Enum<?>[]) type.getEnumConstants();
            buffer.putInt(enumConstants.length);
            for (Enum<?> enumConstant : enumConstants) {
                putString(enumConstant.name(), buffer);
                buffer.putInt(enumConstant instanceof NativeEnum ne ? ne.nativeValue() : enumConstant.ordinal());
            }
        } else {
            buffer.put((byte) (object.isStruct() ? 1 : 0));
            buffer.putInt(object.fields().size());
            for (NativeObject.NativeField field : object.fields()) {
                putString(field.name(), buffer);
                buffer.putInt(getDims(field.type()));
                buffer.putInt(getTypeId(field.type()));
            }
        }
        buffer.putInt(0, buffer.position() - Integer.BYTES);
    }

    /**
     * int : layout entry count
     * for each entry :
     * - int : typeId (component typeId for array, < 0 for primitive)
     * - long : offset
     * - int : bytes size (< 0 if entry is an array, > 0 otherwise)
     * - int : length = 1 or array length
     */
    private void writeLayout(NativeInstancesLayout layout, FileChannel fileChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(layout.entryCount() * LAYOUT_ENTRY_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        writeFully(buffer.putInt(layout.entryCount()).flip(), fileChannel);
        buffer.clear();
        for (LayoutEntry entry : layout) {
            int typeId = getTypeId(entry.instance().getClass());
            buffer.putInt(typeId) // type id
                    .putLong(entry.offset()) // offset
                    .putInt(entry.isArray() ? -entry.size() : entry.size()) // size
                    .putInt(entry.isArray() ? Array.getLength(entry.instance()) : 1); // array lengh
        }
        writeFully(buffer.flip(), fileChannel);
    }

    private void writeInstances(NativeInstancesLayout layout, FileChannel fileChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        // write total size
        writeFully(buffer.putLong(layout.size()).flip(), fileChannel);
        MappedByteBufferAllocation allocation = new MappedByteBufferAllocation(fileChannel, fileChannel.position(), FileChannel.MapMode.READ_WRITE);
        marshaller.marshall(layout, allocation, null);
    }

    private static int getDims(Class<?> type) {
        int count = 0;
        while (type.isArray()) {
            count++;
            type = type.componentType();
        }
        return count;
    }

    private static void putString(String s, ByteBuffer buffer) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(bytes.length).put(bytes);
    }

    private Set<NativeObject> collectReferencedTypes(NativeInstancesLayout layout) {
        Set<NativeObject> referencedTypes = new LinkedHashSet<>();
        for (LayoutEntry entry : layout) {
            if (entry.isArray()) {
                Class<?> componentType = entry.instance().getClass().componentType();
                if (componentType.isPrimitive())
                    continue;
                collectReferencedTypes(componentType, referencedTypes);
            } else {
                collectReferencedTypes(entry.instance().getClass(), referencedTypes);
            }
        }
        return referencedTypes;
    }

    private void collectReferencedTypes(Class<?> type, Set<NativeObject> referencedTypes) {
        NativeObject nativeObject = objectsRegistry.get(type);
        if (!referencedTypes.add(nativeObject))
            return;
        for (NativeObject.NativeField field : nativeObject.fields()) {
            Class<?> fieldType = field.type();
            if (NativeObject.isStruct(fieldType)) {
                collectReferencedTypes(fieldType, referencedTypes);
            } else if (fieldType.isEnum()) {
                NativeObject nativeEnum = objectsRegistry.get(fieldType);
                referencedTypes.add(nativeEnum);
            }
        }
    }

    // < 0 if primitive
    private int getTypeId(Class<?> type) {
        if (type.isArray())
            type = type.componentType();
        if (type.isPrimitive())
            return getPrimitiveTypeId(type);
        NativeObject nativeObject = objectsRegistry.get(type);
        return nativeObject.typeId();
    }

    private static int getPrimitiveTypeId(Class<?> type) {
        if (type == byte.class) return -1;
        if (type == short.class) return -2;
        if (type == int.class) return -3;
        if (type == long.class) return -4;
        if (type == float.class) return -5;
        if (type == double.class) return -6;
        if (type == boolean.class) return -7;
        throw new IllegalArgumentException("Unsupported primitive type " + type.getTypeName());
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void writeFully(ByteBuffer buffer, FileChannel fileChannel) throws IOException {
        while (buffer.hasRemaining())
            fileChannel.write(buffer);
    }
}
