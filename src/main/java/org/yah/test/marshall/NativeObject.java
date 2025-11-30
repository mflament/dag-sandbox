package org.yah.test.marshall;

import com.sun.jna.Structure;

import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class NativeObject {
    public record NativeField(String name, Class<?> type, int offset, int size, int alignment, VarHandle varHandle) {
    }

    private final Class<?> type;
    private final int typeId;
    int size;
    int alignment = 1;
    private final List<NativeField> fields = new ArrayList<>();

    public NativeObject(Class<?> type, int typeId) {
        this.type = Objects.requireNonNull(type, "type is null");
        this.typeId = typeId;
    }

    public NativeObject(Class<?> type, int typeId, int size, int alignment) {
        this.type = type;
        this.typeId = typeId;
        this.size = size;
        this.alignment = alignment;
    }

    public Class<?> type() {
        return type;
    }

    public int typeId() {
        return typeId;
    }

    public int size() {
        return size;
    }

    public int alignment() {
        return alignment;
    }

    public List<NativeField> fields() {
        return fields;
    }

    public boolean isStruct() {
        return isStruct(type);
    }

    public static boolean isStruct(Class<?> type) {
        return Structure.ByValue.class.isAssignableFrom(type);
    }

    @Override
    public String toString() {
        return type.getTypeName();
    }
}
