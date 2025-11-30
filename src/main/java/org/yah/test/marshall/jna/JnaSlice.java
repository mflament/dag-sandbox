package org.yah.test.marshall.jna;

import com.sun.jna.Pointer;
import org.yah.test.marshall.MemorySlice;

public record JnaSlice(Pointer pointer) implements MemorySlice {

    @Override
    public byte readByte(int offset) {
        return pointer.getByte(offset);
    }

    @Override
    public short readShort(int offset) {
        return pointer.getShort(offset);
    }

    @Override
    public int readInt(int offset) {
        return pointer.getInt(offset);
    }

    @Override
    public long readLong(int offset) {
        return pointer.getLong(offset);
    }

    @Override
    public float readFloat(int offset) {
        return pointer.getFloat(offset);
    }

    @Override
    public double readDouble(int offset) {
        return pointer.getDouble(offset);
    }

    @Override
    public void read(int offset, byte[] array) {
        pointer.read(offset, array, 0, array.length);
    }

    @Override
    public void read(int offset, short[] array) {
        pointer.read(offset, array, 0, array.length);
    }

    @Override
    public void read(int offset, int[] array) {
        pointer.read(offset, array, 0, array.length);
    }

    @Override
    public void read(int offset, long[] array) {
        pointer.read(offset, array, 0, array.length);
    }

    @Override
    public void read(int offset, float[] array) {
        pointer.read(offset, array, 0, array.length);
    }

    @Override
    public void read(int offset, double[] array) {
        pointer.read(offset, array, 0, array.length);
    }

    @Override
    public void writeByte(int offset, byte value) {
        pointer.setByte(offset, value);
    }

    @Override
    public void writeShort(int offset, short value) {
        pointer.setShort(offset, value);
    }

    @Override
    public void writeInt(int offset, int value) {
        pointer.setInt(offset, value);
    }

    @Override
    public void writeLong(int offset, long value) {
        pointer.setLong(offset, value);
    }

    @Override
    public void writeFloat(int offset, float value) {
        pointer.setFloat(offset, value);
    }

    @Override
    public void writeDouble(int offset, double value) {
        pointer.setDouble(offset, value);
    }

    @Override
    public void write(int offset, byte[] array) {
        pointer.write(offset, array, 0, array.length);
    }

    @Override
    public void write(int offset, short[] array) {
        pointer.write(offset, array, 0, array.length);
    }

    @Override
    public void write(int offset, int[] array) {
        pointer.write(offset, array, 0, array.length);
    }

    @Override
    public void write(int offset, long[] array) {
        pointer.write(offset, array, 0, array.length);
    }

    @Override
    public void write(int offset, float[] array) {
        pointer.write(offset, array, 0, array.length);
    }

    @Override
    public void write(int offset, double[] array) {
        pointer.write(offset, array, 0, array.length);
    }
}
