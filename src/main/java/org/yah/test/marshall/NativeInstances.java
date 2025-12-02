package org.yah.test.marshall;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class NativeInstances<A extends MemoryAllocation> implements AutoCloseable {
    private final NativeInstancesLayout layout;
    private final A allocation;
    private final @Nullable NativeInstances<?> parent;
    private final List<NativeInstances<?>> children = new ArrayList<>();

    private NativeInstances(NativeInstancesLayout layout, A allocation, @Nullable NativeInstances<?> parent) {
        this.layout = layout;
        this.allocation = allocation;
        this.parent = parent;
    }

    public NativeInstances(NativeInstancesLayout layout, A allocation) {
        this(layout, allocation, null);
    }

    public <CA extends MemoryAllocation> NativeInstances<CA> createChild(NativeInstancesLayout layout, CA allocation) {
        NativeInstances<CA> child = new NativeInstances<>(layout, allocation, this);
        children.add(child);
        return child;
    }

    public NativeInstancesLayout layout() {
        return layout;
    }

    public A allocation() {
        return allocation;
    }

    @Nullable
    public NativeInstances<?> parent() {
        return parent;
    }

    @Override
    public void close() {
        if (!children.isEmpty())
            throw new IllegalStateException("Some child allocation are not closed : " + children.size());
        allocation.close();
        if (parent != null)
            parent.children.remove(this);
    }

    public MemorySlice slice(Object instance) {
        NativeInstancesLayout.LayoutEntry entry = layout.get(instance);
        if (entry == null)
            throw new IllegalArgumentException("No LayoutEntry found for instance " + instance);
        return allocation.slice(entry.offset(), entry.size());
    }

    public boolean contains(Object instance) {
        if (layout.contains(instance))
            return true;
        if (parent != null)
            return parent.contains(instance);
        return false;
    }

    public long addressOf(Object instance) {
        NativeInstances<?> instances = getInstancesContaining(instance);
        if (instances == null)
            return 0L;
        NativeInstancesLayout.LayoutEntry entry = instances.layout.get(instance);
        if (entry == null)
            return 0L;
        long baseAddress = Math.max(1, instances.allocation.address());
        return baseAddress + entry.offset();
    }

    @Nullable
    public Object instanceAtAddress(long address) {
        NativeInstancesLayout.LayoutEntry entry = entryAtAddress(address);
        if (entry == null)
            return null;
        return entry.instance();
    }

    @Nullable
    public NativeInstancesLayout.LayoutEntry entryAtAddress(long address) {
        if (address == 0)
            return null;
        NativeInstances<?> instances = getInstancesAtAddress(address);
        if (instances == null)
            return null;
        long offset = address - Math.max(1, instances.allocation.address());
        return instances.layout.getEntryAtOffset(offset);
    }

    @Nullable
    private NativeInstances<?> getInstancesContaining(Object instance) {
        if (layout.contains(instance))
            return this;
        if (parent != null)
            return parent.getInstancesContaining(instance);
        return null;
    }

    @Nullable
    private NativeInstances<?> getInstancesAtAddress(long address) {
        if (containsAddress(address))
            return this;
        if (parent != null)
            return parent.getInstancesAtAddress(address);
        return null;
    }

    private boolean containsAddress(long address) {
        long base = allocation.address();
        return address >= base && address < base + layout.size();
    }

}
