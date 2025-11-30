package org.yah.test.marshall;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class DefaultTypeIntrospector implements TypeIntrospector {

    public static final TypeIntrospector INSTANCE = new DefaultTypeIntrospector();

    protected DefaultTypeIntrospector() {
    }

    @Override
    public List<Field> getFields(Class<?> type) {
        List<Field> fields = new LinkedList<>();
        collectFields(type, fields);
        return fields;
    }

    private void collectFields(Class<?> type, List<Field> fields) {
        Class<?> superclass = type.getSuperclass();
        if (superclass != null && superclass != Object.class)
            collectFields(superclass, fields);

        Arrays.stream(type.getDeclaredFields())
                .filter(DefaultTypeIntrospector::acceptField)
                .sorted(Comparator.comparing(Field::getName))
                .forEach(fields::add);
    }

    public static boolean acceptField(Field field) {
        int modifiers = field.getModifiers();
        return !Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers) && !Modifier.isFinal(modifiers) && !Modifier.isTransient(modifiers);
    }

}
