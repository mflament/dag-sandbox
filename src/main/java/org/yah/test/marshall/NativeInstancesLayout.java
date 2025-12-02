package org.yah.test.marshall;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.*;

public final class NativeInstancesLayout implements Iterable<NativeInstancesLayout.LayoutEntry> {

    private final NativeObjectsRegistry objectsRegistry;
    private final int alignment;
    private final List<LayoutEntry> entries = new ArrayList<>();
    private IdentityHashMap<Object, LayoutEntry> instanceEntries;
    private Map<Long, LayoutEntry> offsetEntries;

    public NativeInstancesLayout(NativeObjectsRegistry objectsRegistry) {
        this(objectsRegistry, Long.BYTES);
    }

    public NativeInstancesLayout(NativeObjectsRegistry objectsRegistry, int alignment) {
        this.objectsRegistry = Objects.requireNonNull(objectsRegistry, "nativeObjectsRegistry is null");
        this.alignment = alignment;
    }

    public long size() {
        if (entries.isEmpty())
            return 0;
        LayoutEntry last = entries.get(entries.size() - 1);
        return last.offset + last.size;
    }

    public LayoutEntry rootEntry() {
        return entries.get(0);
    }

    @Nonnull
    @Override
    public Iterator<LayoutEntry> iterator() {
        return entries.iterator();
    }

    public int entryCount() {
        return entries.size();
    }

    public record LayoutEntry(Object instance, long offset, int size) {
        public boolean isArray() {
            return instance.getClass().isArray();
        }
    }

    public boolean contains(Object instance) {
        if (instanceEntries == null)
            instanceEntries = createInstanceEntries();
        return instanceEntries.containsKey(instance);
    }

    @Nullable
    public LayoutEntry get(Object instance) {
        if (instanceEntries == null)
            instanceEntries = createInstanceEntries();
        return instanceEntries.get(instance);
    }

    @Nullable
    public LayoutEntry getEntryAtOffset(long offset) {
        if (offsetEntries == null) {
            offsetEntries = createOffsetEntries();
        }
        return offsetEntries.get(offset);
    }

    public boolean add(Object instance) {
        Objects.requireNonNull(instance, "instance is null");
        Class<?> type = instance.getClass();
        if (NativeObjectsRegistry.isValue(type))
            throw new IllegalArgumentException("Can not allocate value type : " + type.getTypeName());

        if (instanceEntries == null)
            instanceEntries = createInstanceEntries();
        else if (instanceEntries.containsKey(instance))
            return false;

        long offset = align(currentOffset(), alignment);
        int size = sizeOfInstance(instance);
        addEntry(new LayoutEntry(instance, offset, size));
        return true;
    }

    void addEntry(LayoutEntry entry) {
        entries.add(entry);
        if (instanceEntries !=null)
            instanceEntries.put(entry.instance(), entry);
        if (offsetEntries != null)
            offsetEntries.put(entry.offset(), entry);
    }

    private IdentityHashMap<Object, LayoutEntry> createInstanceEntries() {
        IdentityHashMap<Object, LayoutEntry> res = new IdentityHashMap<>();
        for (LayoutEntry entry : entries) res.put(entry.instance, entry);
        return res;
    }

    private Map<Long, LayoutEntry> createOffsetEntries() {
        Map<Long, LayoutEntry> map = new HashMap<>(entries.size());
        for (LayoutEntry entry : entries) map.put(entry.offset, entry);
        return map;
    }

    private int sizeOfInstance(Object instance) {
        Class<?> type = instance.getClass();
        if (type.isArray()) {
            int componentSize = objectsRegistry.shallowSizeof(type.componentType());
            return Math.toIntExact(Array.getLength(instance) * (long) componentSize);
        }
        NativeObject nativeObject = objectsRegistry.get(type);
        return nativeObject.size();
    }

    private long currentOffset() {
        if (entries.isEmpty())
            return 0;
        LayoutEntry last = entries.get(entries.size() - 1);
        return last.offset() + last.size();
    }

    static long align(long offset, int alignment) {
        long mod = offset % alignment;
        if (mod > 0)
            return offset + alignment - mod;
        return offset;
    }

}
