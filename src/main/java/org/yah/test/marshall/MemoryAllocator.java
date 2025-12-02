package org.yah.test.marshall;

public interface MemoryAllocator<A extends MemoryAllocation> {

    A allocate(long size);

}
