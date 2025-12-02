package org.yah.test.marshall.foreign;

import org.yah.test.marshall.MemoryAllocation;
import org.yah.test.marshall.MemorySlice;

import javax.annotation.Nullable;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;
import java.util.Objects;

public class MemorySegmentAllocation implements MemoryAllocation {

    private final Arena arena;
    private final MemorySegment memorySegment;
    private final ByteOrder byteOrder;

    public MemorySegmentAllocation(long size) {
        this(size, null);
    }

    public MemorySegmentAllocation(long size, @Nullable ByteOrder byteOrder) {
        arena = Arena.ofShared();
        memorySegment = arena.allocate(size, 1);
        this.byteOrder = Objects.requireNonNullElse(byteOrder, ByteOrder.nativeOrder());
    }

    @Override
    public long address() {
        return memorySegment.address();
    }

    @Override
    public MemorySlice slice(long offset, int size) {
        return new MemorySegmentSlice(memorySegment.asSlice(offset, size, 1), byteOrder);
    }

    @Override
    public void close() {
        arena.close();
    }
}
