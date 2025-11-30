package org.yah.test.marshall;

public interface MemorySlice {

    byte readByte(int offset);

    short readShort(int offset);

    int readInt(int offset);

    long readLong(int offset);

    float readFloat(int offset);

    double readDouble(int offset);

    void read(int offset, byte[] array);

    void read(int offset, short[] array);

    void read(int offset, int[] array);

    void read(int offset, long[] array);

    void read(int offset, float[] array);

    void read(int offset, double[] array);



    void writeByte(int offset, byte value);

    void writeShort(int offset, short value);

    void writeInt(int offset, int value);

    void writeLong(int offset, long value);

    void writeFloat(int offset, float value);

    void writeDouble(int offset, double value);

    void write(int offset, byte[] array);

    void write(int offset, short[] array);

    void write(int offset, int[] array);

    void write(int offset, long[] array);

    void write(int offset, float[] array);

    void write(int offset, double[] array);

}
