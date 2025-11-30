package org.yah.test.marshall;

import com.sun.jna.Structure;
import org.yah.test.marshall.annotations.NativeOrder;

import java.util.List;

// @formatter:off
public class TestObjects {

    public enum TestEnum {
        A, B, C, D
    }

    // alignment 8 size: 80 (+ typeId = 80)
    public static class TestObject {
        @NativeOrder(1) public byte aByte;                                  // offset 0     size: 1
        @NativeOrder(2) public short aShort;                                // offset 2     size: 2
        @NativeOrder(3) public int anInt;                                   // offset 4     size: 4
        @NativeOrder(4) public long aLong;                                  // offset 8     size: 8
        @NativeOrder(5) public float aFloat;                                // offset 16    size: 4
        @NativeOrder(6) public double aDouble;                              // offset 24    size: 8
        @NativeOrder(7) public boolean aBoolean;                            // offset 32    size: 1
        @NativeOrder(8) public TestEnum anEnum = TestEnum.B;                // offset 36    size: 4
        @NativeOrder(9) public float[] aFloatArray;                         // offset 40    size: 8
        @NativeOrder(10) public TestStruct aStruct = new TestStruct();      // offset 48    size: 16
        @NativeOrder(11) public TestObjectWithArrays objectWithArrays;      // offset 64    size 8
        @NativeOrder(12) public TestObject aTestObject;                     // offset 72    size 8
    }

    // alignment 4 size 12
    public static class TestStruct extends Structure implements Structure.ByValue {
        @NativeOrder(0) public int anInt;                                           // offset 0  size 4
        @NativeOrder(2) public float aFloat;                                        // offset 4  size 4
        @NativeOrder(3) public TestNativeEnum aNativeEnum = TestNativeEnum.C;       // offset 8  size 4

        @Override
        protected List<String> getFieldOrder() {
            return List.of("anInt", "aFloat", "aNativeEnum");
        }
    }

    // alignment 8 size 80 (+ type id = 88)
    public static class TestObjectWithArrays {
        @NativeOrder(0) public byte[] aByteArray;                   // offset 0  size 8
        @NativeOrder(1) public short[] aShortArray;                 // offset 8  size 8
        @NativeOrder(2) public int[] anIntArray;                    // offset 16  size 8
        @NativeOrder(3) public long[] aLongArray;                   // offset 24  size 8
        @NativeOrder(4) public float[] aFloatArray;                 // offset 32  size 8
        @NativeOrder(5) public double[] aDoubleArray;               // offset 40  size 8
        @NativeOrder(6) public boolean[] aBooleanArray;             // offset 48  size 8
        @NativeOrder(7) public TestEnum[] anEnumArray;              // offset 56  size 8
        @NativeOrder(8) public TestStruct[] aStructArray;           // offset 64  size 8
        @NativeOrder(9) public Object[] aReferenceArray;            // offset 72  size 8
    }

    public enum TestNativeEnum implements NativeEnum {
        A(5), B(10), C(20), D(42);

        public final int nativeValue;

        TestNativeEnum(int nativeValue) {
            this.nativeValue = nativeValue;
        }

        @Override
        public int nativeValue() {
            return nativeValue;
        }
    }


}
