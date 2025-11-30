package org.yah.test.marshall;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.yah.test.marshall.TestObjects.*;

public abstract class AbstractMarshallerTest {

    protected TestObject createTestObject(Random random) {
        TestObject obj = new TestObject();
        obj.aByte = (byte) random.nextInt(256);
        obj.aShort = (short) random.nextInt(65536);
        obj.anInt = random.nextInt();
        obj.aLong = random.nextLong();
        obj.aFloat = random.nextFloat();
        obj.aDouble = random.nextDouble();
        obj.aBoolean = random.nextBoolean();
        obj.anEnum = randomEnum(random, TestEnum.class);
        obj.aStruct = createTestStruct(random);
        return obj;
    }

    protected TestObjectWithArrays createTestObjectWithArrays(Random random) {
        TestObjectWithArrays toa = new TestObjectWithArrays();

        toa.aByteArray = new byte[randomLength(random)];
        random.nextBytes(toa.aByteArray);

        toa.aShortArray = new short[randomLength(random)];
        for (int i = 0; i < toa.aShortArray.length; i++) toa.aShortArray[i] = (short) random.nextInt(Short.MAX_VALUE);

        toa.anIntArray = new int[randomLength(random)];
        for (int i = 0; i < toa.anIntArray.length; i++) toa.anIntArray[i] = random.nextInt();

        toa.aLongArray = new long[randomLength(random)];
        for (int i = 0; i < toa.aLongArray.length; i++) toa.aLongArray[i] = random.nextLong();

        toa.aFloatArray = new float[randomLength(random)];
        for (int i = 0; i < toa.aFloatArray.length; i++) toa.aFloatArray[i] = random.nextFloat();

        toa.aDoubleArray = new double[randomLength(random)];
        for (int i = 0; i < toa.aDoubleArray.length; i++) toa.aDoubleArray[i] = random.nextDouble();

        toa.aBooleanArray = new boolean[randomLength(random)];
        for (int i = 0; i < toa.aBooleanArray.length; i++) toa.aBooleanArray[i] = random.nextBoolean();

        toa.anEnumArray = new TestEnum[randomLength(random)];
        for (int i = 0; i < toa.anEnumArray.length; i++)
            toa.anEnumArray[i] = randomEnum(random, TestEnum.class);

        toa.aStructArray = new TestStruct[randomLength(random, 5)];
        for (int i = 0; i < toa.aStructArray.length; i++) toa.aStructArray[i] = createTestStruct(random);

        return toa;
    }

    protected TestStruct createTestStruct(Random random) {
        TestStruct testStruct = new TestStruct();
        testStruct.anInt = random.nextInt();
        testStruct.aFloat = random.nextFloat();
        testStruct.aNativeEnum = randomEnum(random, TestNativeEnum.class);
        return testStruct;
    }

    protected void assertTestObject(TestObject expected, TestObject actual) {
        assertEquals(expected.aByte, actual.aByte);
        assertEquals(expected.aShort, actual.aShort);
        assertEquals(expected.anInt, actual.anInt);
        assertEquals(expected.aLong, actual.aLong);
        assertEquals(expected.aFloat, actual.aFloat);
        assertEquals(expected.aDouble, actual.aDouble);
        assertEquals(expected.aBoolean, actual.aBoolean);
        assertEquals(expected.anEnum, actual.anEnum);
        assertTestStruct(expected.aStruct, actual.aStruct);
    }

    protected void assertTestObjectWithArrays(TestObjectWithArrays expected, TestObjectWithArrays actual) {
        assertArrayEquals(expected.aByteArray, actual.aByteArray);
        assertArrayEquals(expected.aShortArray, actual.aShortArray);
        assertArrayEquals(expected.anIntArray, actual.anIntArray);
        assertArrayEquals(expected.aLongArray, actual.aLongArray);
        assertArrayEquals(expected.aFloatArray, actual.aFloatArray);
        assertArrayEquals(expected.aDoubleArray, actual.aDoubleArray);
        assertArrayEquals(expected.aBooleanArray, actual.aBooleanArray);
        assertArrayEquals(expected.anEnumArray, actual.anEnumArray);
        assertEquals(expected.aStructArray.length, actual.aStructArray.length);
        for (int i = 0; i < expected.aStructArray.length; i++) {
            assertTestStruct(expected.aStructArray[i], actual.aStructArray[i]);
        }
    }

    protected void assertTestStruct(TestStruct expected, TestStruct actual) {
        assertEquals(expected.anInt, actual.anInt);
        assertEquals(expected.aFloat, actual.aFloat);
        assertEquals(expected.aNativeEnum, actual.aNativeEnum);
    }

    @SuppressWarnings("SameParameterValue")
    protected static <E extends Enum<?>> E randomEnum(Random random, Class<E> type) {
        E[] enumConstants = type.getEnumConstants();
        return enumConstants[random.nextInt(enumConstants.length)];
    }

    protected static int randomLength(Random random) {
        return randomLength(random, 10);
    }

    protected static int randomLength(Random random, int max) {
        return random.nextInt(1, max + 1);
    }

}
