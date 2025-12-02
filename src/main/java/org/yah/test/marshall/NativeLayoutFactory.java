package org.yah.test.marshall;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.Objects;

public class NativeLayoutFactory {

    private final NativeObjectsRegistry objectsRegistry;
    private final int alignment;

    public NativeLayoutFactory(NativeObjectsRegistry objectsRegistry) {
        this(objectsRegistry, Long.BYTES);
    }

    public NativeLayoutFactory(NativeObjectsRegistry objectsRegistry, int alignment) {
        this.objectsRegistry = Objects.requireNonNull(objectsRegistry, "objectsRegistry is null");
        this.alignment = alignment;
    }

    public NativeInstancesLayout createLayout(Object instance) {
        return createLayout(instance, null);
    }

    public NativeInstancesLayout createLayout(Object instance, @Nullable NativeInstances<?> parentInstances) {
        if (instance == null)
            return null;
        NativeInstancesLayout layout = new NativeInstancesLayout(objectsRegistry, alignment);
        this.createLayout(instance, layout, parentInstances);
        return layout;
    }

    private void createLayout(Object instance, NativeInstancesLayout layout, @Nullable NativeInstances<?> parentInstances) {
        if (instance == null)
            return;

        if (parentInstances != null && parentInstances.contains(instance))
            return;

        if (!layout.add(instance))
            return;

        Class<?> type = instance.getClass();
        if (type.isArray()) {
            int length = Array.getLength(instance);
            if (!NativeObjectsRegistry.isValue(type.componentType())) {
                // create layout entry for each array element
                for (int i = 0; i < length; i++) {
                    this.createLayout(Array.get(instance, i), layout, parentInstances);
                }
            }
        } else {
            NativeObject nativeObject = objectsRegistry.get(type);
            for (NativeObject.NativeField field : nativeObject.fields()) {
                if (!NativeObjectsRegistry.isValue(field.type())) {
                    this.createLayout(field.varHandle().get(instance), layout, parentInstances);
                }
            }
        }
    }

}
