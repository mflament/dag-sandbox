package org.yah.test.marshall.bytebuffer;

import org.yah.test.marshall.MemoryAllocation;
import org.yah.test.marshall.MemorySlice;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MappedByteBufferAllocation implements MemoryAllocation {

    private final FileChannel fileChannel;
    private final long address;
    private final FileChannel.MapMode mapMode;

    public MappedByteBufferAllocation(FileChannel fileChannel, long address, FileChannel.MapMode mapMode) {
        this.fileChannel = fileChannel;
        this.address = address;
        this.mapMode = mapMode;
    }

    @Override
    public long address() {
        return address;
    }

    @Override
    public MemorySlice slice(long offset, int size) {
        try {
            return new ByteBufferSlice(fileChannel.map(mapMode, address + offset, size));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        try {
            fileChannel.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
