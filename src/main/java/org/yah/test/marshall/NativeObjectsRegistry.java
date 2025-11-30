package org.yah.test.marshall;

import org.yah.test.marshall.NativeObject.NativeField;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

public class NativeObjectsRegistry {

    private final Map<Class<?>, NativeObject> nativeObjects = new HashMap<>();

    @Nullable
    private Map<Integer, NativeObject> nativeObjectsByTypeId;

    private final TypeIntrospector typeIntrospector;
    @Nullable
    private final Function<Class<?>, Integer> typeIdSupplier;
    private final MethodHandles.Lookup lookup = MethodHandles.lookup();

    public NativeObjectsRegistry() {
        this(null, null);
    }

    public NativeObjectsRegistry(@Nullable TypeIntrospector typeIntrospector) {
        this(typeIntrospector, null);
    }

    public NativeObjectsRegistry(@Nullable TypeIntrospector typeIntrospector,
                                 @Nullable Function<Class<?>, Integer> typeIdSupplier) {
        this.typeIntrospector = new CachingTypeIntrospector(Objects.requireNonNullElseGet(typeIntrospector, DefaultTypeIntrospector::new));
        this.typeIdSupplier = typeIdSupplier;
    }

    public synchronized NativeObject get(Class<?> type) {
        // reentrant, do not used computeIfAbsent
        NativeObject nativeObject = nativeObjects.get(type);
        if (nativeObject == null) {
            nativeObject = createNativeObject(type);
        }
        return nativeObject;
    }

    public synchronized NativeObject get(int typeId) {
        if (nativeObjectsByTypeId == null) {
            nativeObjectsByTypeId = createNativeObjectsByTypeIds();
        }
        NativeObject nativeObject = nativeObjectsByTypeId.get(typeId);
        if (nativeObject == null)
            throw new NoSuchElementException("Unresolved type id " + typeId);
        return nativeObject;
    }

    private Map<Integer, NativeObject> createNativeObjectsByTypeIds() {
        Map<Integer, NativeObject> res = new HashMap<>();
        nativeObjects.values().forEach(no -> res.put(no.typeId(), no));
        return res;
    }

    public synchronized boolean contains(Class<?> type) {
        return nativeObjects.containsKey(type);
    }

    public synchronized boolean contains(int typeId) {
        if (nativeObjectsByTypeId == null) {
            nativeObjectsByTypeId = createNativeObjectsByTypeIds();
        }
        return nativeObjectsByTypeId.containsKey(typeId);
    }

    private NativeObject createNativeObject(Class<?> type) {
        if (type.isPrimitive())
            throw new IllegalArgumentException("Can not create NativeObject from primitive : " + type.getTypeName());
        if (type.isArray())
            throw new IllegalArgumentException("Can not create NativeObject from array : " + type.getTypeName());

        int typeId;
        if (typeIdSupplier == null)
            typeId = nativeObjects.size();
        else
            typeId = Objects.requireNonNull(typeIdSupplier.apply(type), "no type id for type : " + type.getTypeName());

        if (type.isEnum()) {
            NativeObject nativeEnum = new NativeObject(type, typeId, Integer.BYTES, Integer.BYTES);
            nativeObjects.put(type, nativeEnum);
            return nativeEnum;
        }

        NativeObject nativeObject = new NativeObject(type, typeId);
        nativeObjects.put(type, nativeObject);

        List<Field> fields = typeIntrospector.getFields(type);
        int offset = 0;
        if (!nativeObject.isStruct()) {
            offset = nativeObject.alignment = Long.BYTES; // type id
        }
        for (Field field : fields) {
            Class<?> fieldType = field.getType();
            int fieldSize = shallowSizeof(fieldType);
            int fieldAlignment = getAlignment(fieldType);
            offset = align(offset, fieldAlignment);
            nativeObject.alignment = Math.max(nativeObject.alignment, fieldAlignment);
            VarHandle varHandle = getVarHandle(field);
            nativeObject.fields().add(new NativeField(field.getName(), fieldType, offset, fieldSize, fieldAlignment, varHandle));
            offset += fieldSize;

            while (fieldType.isArray())
                fieldType = fieldType.componentType();

            if (fieldType.isEnum())
                get(fieldType);

            if (hasNativeField(fieldType))
                get(fieldType);
        }
        nativeObject.size = align(offset, nativeObject.alignment);
        return nativeObject;
    }

    private boolean hasNativeField(Class<?> fieldType) {
        return !typeIntrospector.getFields(fieldType).isEmpty();
    }

    private VarHandle getVarHandle(Field field) {
        try {
            return lookup.unreflectVarHandle(field);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isValue(Class<?> type) {
        return type.isPrimitive() || type.isEnum() || NativeObject.isStruct(type);
    }

    private int getAlignment(Class<?> type) {
        if (NativeObject.isStruct(type)) {
            NativeObject nativeObject = get(type);
            return nativeObject.alignment();
        }
        return shallowSizeof(type);
    }

    public int shallowSizeof(Class<?> type) {
        if (type.isPrimitive())
            return sizeofPrimitive(type);

        if (type.isEnum())
            return Integer.BYTES;

        if (NativeObject.isStruct(type)) {
            NativeObject nativeObject = get(type);
            return nativeObject.size();
        }

        // every thing else is pointer (fixed size array in struct are not supported)
        return Long.BYTES;
    }

    static int align(int offset, int alignment) {
        int mod = offset % alignment;
        if (mod > 0)
            return offset + alignment - mod;
        return offset;
    }

    public static int sizeofPrimitive(Class<?> type) {
        if (type == byte.class) return Byte.BYTES;
        if (type == short.class) return Short.BYTES;
        if (type == int.class) return Integer.BYTES;
        if (type == long.class) return Long.BYTES;
        if (type == float.class) return Float.BYTES;
        if (type == double.class) return Double.BYTES;
        if (type == boolean.class) return Byte.BYTES;
        throw new IllegalArgumentException(type.getTypeName());
    }

    public void add(NativeObject nativeObject) {
        nativeObjects.put(nativeObject.type(), nativeObject);
    }
}
