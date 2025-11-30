package org.yah.test.marshall;

import java.lang.reflect.Field;
import java.util.List;

@FunctionalInterface
public interface TypeIntrospector {
    List<Field> getFields(Class<?> type);
}
