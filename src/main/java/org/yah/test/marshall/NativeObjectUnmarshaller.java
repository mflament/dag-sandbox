package org.yah.test.marshall;

import org.yah.test.marshall.NativeInstancesLayout.LayoutEntry;
import org.yah.test.marshall.NativeObject.NativeField;
import org.yah.test.marshall.NativeObjectMarshaller.NativeInstances;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;

import static org.yah.test.marshall.NativeObjectMarshaller.formatFieldName;
import static org.yah.test.marshall.NativeObjectsRegistry.align;

public final class NativeObjectUnmarshaller {

    private final NativeObjectsRegistry objectsRegistry;

    public NativeObjectUnmarshaller(NativeObjectsRegistry objectsRegistry) {
        this.objectsRegistry = Objects.requireNonNull(objectsRegistry, "objectsRegistry is null");
    }

    public Object unmarshall(NativeInstances<?> instances) {
        LayoutEntry entry = instances.layout().rootEntry();
        unmarshall(instances, entry.instance());
        return entry.instance();
    }

    public void unmarshall(NativeInstances<?> instances, Object instance) {
        UnmarshallingContext<?> ctx = UnmarshallingContext.create(instances, instance);
        unmarshall(ctx);

        while (!ctx.queue.isEmpty()) {
            unmarshall(ctx.queue.pop());
        }
    }

    private void unmarshall(UnmarshallingContext<?> ctx) {
        LayoutEntry entry = ctx.entry;
        if (!ctx.unmarshalled.add(entry))
            return;

        Object instance = entry.instance();
        ctx.push(instance);
        if (entry.isArray())
            unmarshallArray(instance, ctx);
        else
            unmarshallObject(instance, ctx);
        ctx.pop();
    }

    private void unmarshallArray(Object instance, UnmarshallingContext<?> ctx) {
        Class<?> componentType = instance.getClass().componentType();
        if (componentType.isPrimitive())
            unmarshallPrimitiveArray(instance, ctx);
        else if (componentType.isEnum())
            unmarshallEnumArray(instance, ctx);
        else if (NativeObject.isStruct(componentType))
            unmarshallStructArray(instance, ctx);
        else
            unmarshallPointerArray(instance, ctx);
    }

    private void unmarshallPrimitiveArray(Object instance, UnmarshallingContext<?> ctx) {
        MemorySlice slice = ctx.slice;
        if (instance instanceof byte[] a) slice.read(0, a);
        else if (instance instanceof short[] a) slice.read(0, a);
        else if (instance instanceof int[] a) slice.read(0, a);
        else if (instance instanceof long[] a) slice.read(0, a);
        else if (instance instanceof float[] a) slice.read(0, a);
        else if (instance instanceof double[] a) slice.read(0, a);
        else if (instance instanceof boolean[] a) {
            byte[] bytes = new byte[a.length];
            slice.read(0, bytes);
            for (int i = 0; i < bytes.length; i++) {
                a[i] = bytes[i] != 0;
            }
        }
    }

    private void unmarshallEnumArray(Object instance, UnmarshallingContext<?> ctx) {
        Enum<?>[] enums = (Enum<?>[]) instance;
        int[] values = new int[enums.length];
        ctx.slice.read(0, values);
        Class<?> enumType = instance.getClass().componentType();
        for (int i = 0; i < values.length; i++) {
            enums[i] = resolveEnum(enumType, values[i]);
        }
    }

    private Enum<?> resolveEnum(Class<?> enumType, int value) {
        if (NativeEnum.class.isAssignableFrom(enumType)) {
            return NativeEnum.resolve(enumType, value);
        }
        return (Enum<?>) enumType.getEnumConstants()[value];
    }

    private void unmarshallStructArray(Object instance, UnmarshallingContext<?> ctx) {
        NativeObject nativeObject = objectsRegistry.get(instance.getClass().componentType());
        int length = Array.getLength(instance);
        int offset = 0;
        for (int i = 0; i < length; i++) {
            ctx.push(i);
            offset = align(offset, nativeObject.alignment());
            Object struct = Array.get(instance, i);
            readFields(struct, nativeObject, offset, ctx);
            offset += nativeObject.size();
            ctx.pop();
        }
    }

    private void unmarshallPointerArray(Object instance, UnmarshallingContext<?> ctx) {
        int length = Array.getLength(instance);
        long[] addresses = new long[length];
        ctx.slice.read(0, addresses);
        for (int i = 0; i < addresses.length; i++) {
            ctx.push(i);
            long address = addresses[i];
            Object component = ctx.submit(address);
            Array.set(instance, i, component);
            ctx.pop();
        }
    }

    private void unmarshallObject(Object instance, UnmarshallingContext<?> ctx) {
        Class<?> type = instance.getClass();
        NativeObject nativeObject = objectsRegistry.get(type);
        long typeId = ctx.slice.readLong(0);
        assert typeId == nativeObject.typeId();
        readFields(instance, nativeObject, 0, ctx);
    }

    private void readFields(Object instance, NativeObject nativeObject, int baseOffset, UnmarshallingContext<?> ctx) {
        for (NativeField field : nativeObject.fields()) {
            ctx.push(field);
            try {
                int offset = align(baseOffset + field.offset(), field.alignment());
                readField(instance, field, offset, ctx);
            } catch (RuntimeException e) {
                throw new RuntimeException("Error unmarshalling field " + formatFieldName(instance, field, ctx.reflectionPath), e);
            }
            ctx.pop();
        }
    }

    private void readField(Object instance, NativeField field, int offset, UnmarshallingContext<?> ctx) {
        Class<?> fieldType = field.type();
        if (fieldType.isPrimitive()) {
            field.varHandle().set(instance, readPrimitive(fieldType, ctx.slice, offset));
        } else if (fieldType.isEnum()) {
            field.varHandle().set(instance, readEnum(fieldType, ctx.slice, offset));
        } else if (NativeObject.isStruct(fieldType)) {
            Object struct = field.varHandle().get(instance);
            NativeObject nativeObject = objectsRegistry.get(fieldType);
            readFields(struct, nativeObject, offset, ctx);
        } else {
            long address = ctx.slice.readLong(offset);
            field.varHandle().set(instance, ctx.submit(address));
        }
    }

    private Enum<?> readEnum(Class<?> type, MemorySlice slice, int offset) {
        int value = slice.readInt(offset);
        return resolveEnum(type, value);
    }

    private static Object readPrimitive(Class<?> type, MemorySlice slice, int offset) {
        if (type == byte.class) return slice.readByte(offset);
        if (type == short.class) return slice.readShort(offset);
        if (type == int.class) return slice.readInt(offset);
        if (type == long.class) return slice.readLong(offset);
        if (type == float.class) return slice.readFloat(offset);
        if (type == double.class) return slice.readDouble(offset);
        if (type == boolean.class) return slice.readByte(offset) != 0;
        throw new IllegalArgumentException("Invalid primitive type " + type.getTypeName());
    }

    private record UnmarshallingContext<A extends MemoryAllocation>(NativeInstances<A> instances, LayoutEntry entry,
                                                                    MemorySlice slice,
                                                                    Set<LayoutEntry> unmarshalled,
                                                                    LinkedList<UnmarshallingContext<A>> queue,
                                                                    @Nullable ReflectionPath reflectionPath) {

        static <A extends MemoryAllocation> UnmarshallingContext<A> create(NativeInstances<A> instances, Object instance) {
            LayoutEntry entry = instances.layout().get(instance);
            return new UnmarshallingContext<>(instances, entry, instances.slice(instance), new HashSet<>(), new LinkedList<>(),
                    NativeObjectMarshaller.DEBUG ? new ReflectionPath() : null);
        }

        private UnmarshallingContext<A> createNextContext(LayoutEntry entry) {
            MemorySlice slice = instances.allocation().slice(entry.offset(), entry.size());
            ReflectionPath reflectionPath = this.reflectionPath != null ? new ReflectionPath(this.reflectionPath) : null;
            return new UnmarshallingContext<>(instances, entry, slice, unmarshalled, queue, reflectionPath);
        }

        public Object submit(long address) {
            if (address == 0)
                return null;

            long offset = address - Math.max(1, instances.allocation().address());
            LayoutEntry entry = instances.layout().getEntryAtOffset(offset);
            queue.add(createNextContext(entry));
            return entry.instance();
        }

        public void push(Object instance) {
            if (reflectionPath != null) reflectionPath.push(instance);
        }

        public void pop() {
            if (reflectionPath != null) reflectionPath.pop();
        }

        @Nonnull
        @Override
        public String toString() {
            return entry.instance().toString();
        }
    }

}
