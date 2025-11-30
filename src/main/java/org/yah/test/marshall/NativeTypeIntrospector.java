package org.yah.test.marshall;

import org.yah.test.marshall.annotations.NativeOrder;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

public class NativeTypeIntrospector extends DefaultTypeIntrospector {

    public static final TypeIntrospector INSTANCE = new NativeTypeIntrospector();

    protected NativeTypeIntrospector() {
    }

    @Override
    public List<Field> getFields(Class<?> type) {
        return super.getFields(type).stream()
                .map(NativeTypeIntrospector::createFieldWithOrder)
                .filter(Objects::nonNull)
                .sorted()
                .map(FieldWithOrder::field)
                .toList();
    }

    @Nullable
    private static FieldWithOrder createFieldWithOrder(Field field) {
        NativeOrder annotation = field.getAnnotation(NativeOrder.class);
        if (annotation == null)
            return null;
        return new FieldWithOrder(field, annotation.value());
    }

    private record FieldWithOrder(Field field, int nativeOrder) implements Comparable<FieldWithOrder> {
        @Override
        public int compareTo(FieldWithOrder o) {
            return Integer.compare(nativeOrder, o.nativeOrder);
        }
    }

}
