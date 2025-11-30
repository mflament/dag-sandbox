package org.yah.test.marshall.bytebuffer;

import org.yah.test.marshall.MemorySlice;

import java.nio.ByteBuffer;

public record ByteBufferSlice(ByteBuffer buffer) implements MemorySlice {

    @Override
    public byte readByte(int offset) {
        return buffer.get(offset);
    }

    @Override
    public short readShort(int offset) {
        return buffer.getShort(offset);
    }

    @Override
    public int readInt(int offset) {
        return buffer.getInt(offset);
    }

    @Override
    public long readLong(int offset) {
        return buffer.getLong(offset);
    }

    @Override
    public float readFloat(int offset) {
        return buffer.getFloat(offset);
    }

    @Override
    public double readDouble(int offset) {
        return buffer.getDouble(offset);
    }

    @Override
    public void read(int offset, byte[] array) {
        buffer.get(offset, array);
    }

    @Override
    public void read(int offset, short[] array) {
        buffer.slice(offset, array.length * Short.BYTES).asShortBuffer().get(array);
    }

    @Override
    public void read(int offset, int[] array) {
        buffer.slice(offset, array.length * Integer.BYTES).asIntBuffer().get(array);
    }

    @Override
    public void read(int offset, long[] array) {
        buffer.slice(offset, array.length * Long.BYTES).asLongBuffer().get(array);
    }

    @Override
    public void read(int offset, float[] array) {
        buffer.slice(offset, array.length * Float.BYTES).asFloatBuffer().get(array);
    }

    @Override
    public void read(int offset, double[] array) {
        buffer.slice(offset, array.length * Double.BYTES).asDoubleBuffer().get(array);
    }

    @Override
    public void writeByte(int offset, byte value) {
        buffer.put(offset, value);
    }

    @Override
    public void writeShort(int offset, short value) {
        buffer.putShort(offset, value);
    }

    @Override
    public void writeInt(int offset, int value) {
        buffer.putInt(offset, value);
    }

    @Override
    public void writeLong(int offset, long value) {
        buffer.putLong(offset, value);
    }

    @Override
    public void writeFloat(int offset, float value) {
        buffer.putFloat(offset, value);
    }

    @Override
    public void writeDouble(int offset, double value) {
        buffer.putDouble(offset, value);
    }

    @Override
    public void write(int offset, byte[] array) {
        buffer.put(offset, array);
    }

    @Override
    public void write(int offset, short[] array) {
        buffer.slice(offset, array.length * Short.BYTES).asShortBuffer().put(array);
    }

    @Override
    public void write(int offset, int[] array) {
        buffer.slice(offset, array.length * Integer.BYTES).asIntBuffer().put(array);
    }

    @Override
    public void write(int offset, long[] array) {
        buffer.slice(offset, array.length * Long.BYTES).asLongBuffer().put(array);
    }

    @Override
    public void write(int offset, float[] array) {
        buffer.slice(offset, array.length * Float.BYTES).asFloatBuffer().put(array);
    }

    @Override
    public void write(int offset, double[] array) {
        buffer.slice(offset, array.length * Double.BYTES).asDoubleBuffer().put(array);
    }
}
