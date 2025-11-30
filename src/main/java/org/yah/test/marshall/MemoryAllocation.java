package org.yah.test.marshall;

public interface MemoryAllocation extends AutoCloseable {

    long address();

    MemorySlice slice(long offset, int size);

    @Override
    void close();

}
