package org.yah.test.marshall.bytebuffer;

import org.yah.test.marshall.MemoryAllocation;
import org.yah.test.marshall.MemorySlice;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.LongFunction;
import java.util.function.Supplier;

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
            MappedByteBuffer buffer = fileChannel.map(mapMode, address + offset, size);
            return new ByteBufferSlice(buffer);
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
