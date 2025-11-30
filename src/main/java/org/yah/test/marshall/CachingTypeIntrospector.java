package org.yah.test.marshall;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CachingTypeIntrospector implements TypeIntrospector {
    private final TypeIntrospector delegate;
    private final Map<Class<?>,  List<Field>> cache = new HashMap<>();

    public CachingTypeIntrospector(TypeIntrospector delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate is null");
    }

    @Override
    public synchronized List<Field> getFields(Class<?> type) {
        List<Field> fields = cache.get(type);
        if (fields == null) {
            fields = delegate.getFields(type);
            cache.put(type, fields);
        }
        return fields;
    }
}
