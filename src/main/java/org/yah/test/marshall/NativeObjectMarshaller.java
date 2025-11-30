package org.yah.test.marshall;

import org.yah.test.marshall.NativeInstancesLayout.LayoutEntry;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.Map;
import java.util.Objects;

import static org.yah.test.marshall.NativeObject.NativeField;
import static org.yah.test.marshall.NativeObjectsRegistry.align;

public final class NativeObjectMarshaller {

    static final boolean DEBUG = true;

    public record NativeInstances<A extends MemoryAllocation>(NativeInstancesLayout layout, A allocation) implements AutoCloseable {
        public MemorySlice slice(Object instance) {
            LayoutEntry entry = layout.get(instance);
            return allocation.slice(entry.offset(), entry.size());
        }

        @Override
        public void close() {
            allocation.close();
        }
    }

    private final NativeObjectsRegistry objectsRegistry;
    private final NativeLayoutFactory layoutFactory;

    public NativeObjectMarshaller(NativeObjectsRegistry objectsRegistry) {
        this.objectsRegistry = Objects.requireNonNull(objectsRegistry, "objectsRegistry is null");
        this.layoutFactory = new NativeLayoutFactory(objectsRegistry);
    }

    public <A extends MemoryAllocation> NativeInstances<A> marshall(Object instance, MemoryAllocator<A> allocator) {
        NativeInstancesLayout layout = layoutFactory.createLayout(instance);
        return marshall(layout, allocator.allocate(layout.size()));
    }

    public <A extends MemoryAllocation> NativeInstances<A> marshall(NativeInstancesLayout layout, A allocation) {
        for (LayoutEntry layoutEntry : layout) {
            MemorySlice slice = allocation.slice(layoutEntry.offset(), layoutEntry.size());
            MarshallingContext ctx = new MarshallingContext(layout, allocation.address(), slice, DEBUG ? new ReflectionPath() : null);
            marshall(layoutEntry, ctx);
        }
        return new NativeInstances<>(layout, allocation);
    }

    private record MarshallingContext(NativeInstancesLayout layout, long baseAddress, MemorySlice slice,
                                      @Nullable ReflectionPath reflectionPath) {

        public long addressOf(Object instance) {
            if (instance == null)
                return 0L;
            LayoutEntry layoutEntry = layout.get(instance);
            return Math.max(1, baseAddress) + layoutEntry.offset();
        }

        public void push(Object instance) {
            if (reflectionPath != null) reflectionPath.push(instance);
        }

        public void pop() {
            if (reflectionPath != null) reflectionPath.pop();
        }
    }

    private void marshall(LayoutEntry entry, MarshallingContext ctx) {
        Object instance = entry.instance();
        ctx.push(instance);
        if (entry.isArray()) {
            marshallArray(instance, ctx);
        } else {
            marshallObject(instance, ctx);
        }
        ctx.pop();
    }

    private void marshallArray(Object instance, MarshallingContext ctx) {
        Class<?> componentType = instance.getClass().componentType();
        if (componentType.isPrimitive()) {
            marshallPrimitiveArray(instance, ctx);
        } else if (componentType.isEnum()) {
            marshallEnumArray(instance, ctx);
        } else if (NativeObject.isStruct(componentType)) {
            marshallStructArray(instance, ctx);
        } else {
            marshallPointerArray(instance, ctx);
        }
    }

    private void marshallPrimitiveArray(Object instance, MarshallingContext ctx) {
        MemorySlice slice = ctx.slice;
        if (instance instanceof byte[] a) slice.write(0, a);
        else if (instance instanceof short[] a) slice.write(0, a);
        else if (instance instanceof int[] a) slice.write(0, a);
        else if (instance instanceof long[] a) slice.write(0, a);
        else if (instance instanceof float[] a) slice.write(0, a);
        else if (instance instanceof double[] a) slice.write(0, a);
        else if (instance instanceof boolean[] a) {
            byte[] values = new byte[a.length];
            for (int i = 0; i < a.length; i++) values[i] = (byte) (a[i] ? 1 : 0);
            slice.write(0, values);
        } else
            throw new IllegalArgumentException("Unsupported primitive type " + instance.getClass().getTypeName());
    }

    private void marshallEnumArray(Object instance, MarshallingContext ctx) {
        // resolve enum type to add it to NativeObjectsRegistry
        objectsRegistry.get(instance.getClass().componentType());

        Enum<?>[] enums = (Enum<?>[]) instance;
        for (int i = 0; i < enums.length; i++) {
            marshallEnum(enums[i], ctx, i * Integer.BYTES);
        }
    }

    private void marshallEnum(Enum<?> e, MarshallingContext ctx, int offset) {
        if (e == null) throw new IllegalArgumentException("enum can not be null");
        int value = e instanceof NativeEnum ne ? ne.nativeValue() : e.ordinal();
        ctx.slice.writeInt(offset, value);
    }

    private void marshallStructArray(Object instance, MarshallingContext ctx) {
        Class<?> componentType = instance.getClass().componentType();
        NativeObject nativeObject = objectsRegistry.get(componentType);
        int length = Array.getLength(instance);
        int offset = 0;
        for (int i = 0; i < length; i++) {
            ctx.push(i);
            offset = align(offset, nativeObject.alignment());
            Object struct = Array.get(instance, i);
            if (struct == null)
                throw new IllegalArgumentException("struct array component can not be null");
            marshallFields(struct, offset, nativeObject, ctx);
            offset += nativeObject.size();
            ctx.pop();
        }
    }

    private void marshallPointerArray(Object instance, MarshallingContext ctx) {
        int length = Array.getLength(instance);
        long[] addresses = new long[length];
        for (int i = 0; i < length; i++) {
            Object component = Array.get(instance, i);
            addresses[i] = ctx.addressOf(component);
        }
        ctx.slice.write(0, addresses);
    }

    private void marshallObject(Object instance, MarshallingContext ctx) {
        NativeObject nativeObject = objectsRegistry.get(instance.getClass());
        ctx.slice.writeLong(0, nativeObject.typeId());
        marshallFields(instance, 0, nativeObject, ctx);
    }

    private void marshallFields(Object instance, int baseOffset, NativeObject nativeObject, MarshallingContext ctx) {
        MemorySlice slice = ctx.slice;
        for (NativeField field : nativeObject.fields()) {
            ctx.push(field);
            Class<?> type = field.type();
            try {
                Object value = field.varHandle().get(instance);
                int offset = baseOffset + field.offset();
                if (type.isPrimitive()) {
                    marshallPrimitive(value, slice, offset);
                } else if (type.isEnum()) {
                    marshallEnum((Enum<?>) value, ctx, offset);
                } else if (NativeObject.isStruct(type)) {
                    if (value == null)
                        throw new IllegalStateException("structure by value field " + formatFieldName(instance, field, ctx.reflectionPath) + " can not be null");
                    NativeObject nativeStruct = objectsRegistry.get(type);
                    marshallFields(value, offset, nativeStruct, ctx);
                } else {
                    slice.writeLong(offset, ctx.addressOf(value));
                }
            } catch (RuntimeException e) {
                throw new RuntimeException("Error marshalling field " + formatFieldName(instance, field, ctx.reflectionPath), e);
            }
            ctx.pop();
        }
    }

    private void marshallPrimitive(Object value, MemorySlice slice, int offset) {
        if (value instanceof Byte v) slice.writeByte(offset, v);
        else if (value instanceof Short v) slice.writeShort(offset, v);
        else if (value instanceof Integer v) slice.writeInt(offset, v);
        else if (value instanceof Long v) slice.writeLong(offset, v);
        else if (value instanceof Float v) slice.writeFloat(offset, v);
        else if (value instanceof Double v) slice.writeDouble(offset, v);
        else if (value instanceof Boolean v) slice.writeByte(offset, (byte) (v ? 1 : 0));
        else throw new IllegalArgumentException("Invalid primitive type " + value.getClass().getTypeName());
    }

    static String formatFieldName(Object instance, NativeField field, @Nullable ReflectionPath reflectionPath) {
        String fieldName;
        if (reflectionPath != null)
            return reflectionPath.toString();
        return instance.getClass().getTypeName() + "#" + field.name();
    }

}
