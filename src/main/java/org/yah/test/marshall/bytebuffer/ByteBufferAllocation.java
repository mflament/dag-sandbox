package org.yah.test.marshall.bytebuffer;

import org.yah.test.marshall.MemoryAllocation;
import org.yah.test.marshall.MemorySlice;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ByteBufferAllocation implements MemoryAllocation {

    public static ByteBufferAllocation createDirect(long size) {
        return createDirect(size, null);
    }

    public static ByteBufferAllocation createDirect(long size, @Nullable ByteOrder byteOrder) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(Math.toIntExact(size))
                .order(byteOrder != null ? byteOrder : ByteOrder.nativeOrder());
        long address = getBufferAddress(buffer);
        return new ByteBufferAllocation(buffer, address);
    }

    public static ByteBufferAllocation create(long size) {
        return create(size, 0, null);
    }

    public static ByteBufferAllocation create(long size, @Nullable ByteOrder byteOrder) {
        return create(size, 0, byteOrder);
    }

    public static ByteBufferAllocation create(long size, long address, @Nullable ByteOrder byteOrder) {
        ByteBuffer buffer = ByteBuffer.allocate(Math.toIntExact(size))
                .order(byteOrder != null ? byteOrder : ByteOrder.nativeOrder());
        return new ByteBufferAllocation(buffer, address);
    }

    private static volatile VarHandle addressVarHandle;

    private static long getBufferAddress(ByteBuffer buffer) {
        if (addressVarHandle == null) {
            createAddressHandle();
        }
        return (long) addressVarHandle.get(buffer);
    }

    private synchronized static void createAddressHandle() {
        if (addressVarHandle == null) {
            try {
                MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(Buffer.class, MethodHandles.lookup());
                addressVarHandle = lookup.findVarHandle(Buffer.class, "address", long.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    private ByteBuffer buffer;
    private long address;

    private ByteBufferAllocation(ByteBuffer buffer, long address) {
        this.buffer = buffer;
        this.address = address;
    }

    @Override
    public long address() {
        return address;
    }

    @Override
    public MemorySlice slice(long offset, int size) {
        return new ByteBufferSlice(buffer.slice(Math.toIntExact(offset), size));
    }

    @Override
    public void close() {
        buffer = null;
        address = 0;
    }
}
