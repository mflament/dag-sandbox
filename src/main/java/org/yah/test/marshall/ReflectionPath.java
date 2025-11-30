package org.yah.test.marshall;

import org.yah.test.marshall.NativeObject.NativeField;

import java.lang.reflect.Field;
import java.util.LinkedList;

public final class ReflectionPath {
    private final LinkedList<Object> path = new LinkedList<>();

    public ReflectionPath() {
    }

    public ReflectionPath(ReflectionPath from) {
        path.addAll(from.path);
    }

    public void push(Object o) {
        path.add(o);
    }

    public void pop() {
        path.removeLast();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        Object previous = null;
        for (Object o : path) {
            if (o instanceof Integer index) {
                sb.append('[').append(index).append(']');
            } else if (o instanceof NativeField field) {
                if (previous != null) sb.append('.');
                sb.append(field.name());
            } else if (previous == null) {
                sb.append(o.getClass().getTypeName());
            }
            previous = o;
        }
        return sb.toString();
    }

}
