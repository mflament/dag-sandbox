package org.yah.test.marshall.foreign;

import org.yah.test.marshall.MemorySlice;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.util.Objects;

public final class MemorySegmentSlice implements MemorySlice {
    private final MemorySegment memorySegment;
    private final ByteOrder byteOrder;

    public MemorySegmentSlice(MemorySegment memorySegment, ByteOrder byteOrder) {
        this.memorySegment = Objects.requireNonNull(memorySegment, "memorySegment is null");
        this.byteOrder = Objects.requireNonNull(byteOrder, "byteOrder is null");
    }

    @Override
    public byte readByte(int offset) {
        return memorySegment.get(ValueLayout.JAVA_BYTE, offset);
    }

    @Override
    public short readShort(int offset) {
        return memorySegment.get(ValueLayout.JAVA_SHORT_UNALIGNED, offset);
    }

    @Override
    public int readInt(int offset) {
        return memorySegment.get(ValueLayout.JAVA_INT_UNALIGNED, offset);
    }

    @Override
    public long readLong(int offset) {
        return memorySegment.get(ValueLayout.JAVA_LONG_UNALIGNED, offset);
    }

    @Override
    public float readFloat(int offset) {
        return memorySegment.get(ValueLayout.JAVA_FLOAT_UNALIGNED, offset);
    }

    @Override
    public double readDouble(int offset) {
        return memorySegment.get(ValueLayout.JAVA_DOUBLE_UNALIGNED, offset);
    }

    @Override
    public void read(int offset, byte[] array) {
        memorySegment.asSlice(offset).asByteBuffer().get(array);
    }

    @Override
    public void read(int offset, short[] array) {
        memorySegment.asSlice(offset).asByteBuffer().order(byteOrder).asShortBuffer().get(array);
    }

    @Override
    public void read(int offset, int[] array) {
        memorySegment.asSlice(offset).asByteBuffer().order(byteOrder).asIntBuffer().get(array);
    }

    @Override
    public void read(int offset, long[] array) {
        memorySegment.asSlice(offset).asByteBuffer().order(byteOrder).asLongBuffer().get(array);
    }

    @Override
    public void read(int offset, float[] array) {
        memorySegment.asSlice(offset).asByteBuffer().order(byteOrder).asFloatBuffer().get(array);
    }

    @Override
    public void read(int offset, double[] array) {
        memorySegment.asSlice(offset).asByteBuffer().order(byteOrder).asDoubleBuffer().get(array);
    }

    @Override
    public void writeByte(int offset, byte value) {
        memorySegment.set(ValueLayout.JAVA_BYTE, offset, value);
    }

    @Override
    public void writeShort(int offset, short value) {
        memorySegment.set(ValueLayout.JAVA_SHORT_UNALIGNED, offset, value);
    }

    @Override
    public void writeInt(int offset, int value) {
        memorySegment.set(ValueLayout.JAVA_INT_UNALIGNED, offset, value);
    }

    @Override
    public void writeLong(int offset, long value) {
        memorySegment.set(ValueLayout.JAVA_LONG_UNALIGNED, offset, value);
    }

    @Override
    public void writeFloat(int offset, float value) {
        memorySegment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, offset, value);
    }

    @Override
    public void writeDouble(int offset, double value) {
        memorySegment.set(ValueLayout.JAVA_DOUBLE_UNALIGNED, offset, value);
    }

    @Override
    public void write(int offset, byte[] array) {
        memorySegment.asSlice(offset).asByteBuffer().put(array);
    }

    @Override
    public void write(int offset, short[] array) {
        memorySegment.asSlice(offset).asByteBuffer().order(byteOrder).asShortBuffer().put(array);
    }

    @Override
    public void write(int offset, int[] array) {
        memorySegment.asSlice(offset).asByteBuffer().order(byteOrder).asIntBuffer().put(array);
    }

    @Override
    public void write(int offset, long[] array) {
        memorySegment.asSlice(offset).asByteBuffer().order(byteOrder).asLongBuffer().put(array);
    }

    @Override
    public void write(int offset, float[] array) {
        memorySegment.asSlice(offset).asByteBuffer().order(byteOrder).asFloatBuffer().put(array);
    }

    @Override
    public void write(int offset, double[] array) {
        memorySegment.asSlice(offset).asByteBuffer().order(byteOrder).asDoubleBuffer().put(array);
    }
}
