package org.yah.test.marshall.jna;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import org.yah.test.marshall.MemoryAllocation;
import org.yah.test.marshall.MemorySlice;

public record JnaAllocation(Memory memory) implements MemoryAllocation, AutoCloseable {

    public JnaAllocation(long size) {
        this(new Memory(size));
    }

    @Override
    public void close() {
        memory.close();
    }

    @Override
    public long address() {
        return Pointer.nativeValue(memory);
    }

    @Override
    public MemorySlice slice(long offset, int size) {
        return new JnaSlice(memory.share(offset, size));
    }

}
