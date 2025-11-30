package org.yah.test.marshall;

import org.junit.jupiter.api.Test;
import org.yah.test.marshall.annotations.NativeOrder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.yah.test.marshall.TestObjects.*;

class NativeObjectsRegistryTest {

    @Test
    void isValue() {
        assertTrue(NativeObjectsRegistry.isValue(TestEnum.class));
        assertTrue(NativeObjectsRegistry.isValue(int.class));
        assertTrue(NativeObjectsRegistry.isValue(TestStruct.class));
        assertFalse(NativeObjectsRegistry.isValue(TestObject.class));
        assertFalse(NativeObjectsRegistry.isValue(int[].class));
    }

    @Test
    void getWithoutTypeIdSupplier() {
        NativeObjectsRegistry nor = new NativeObjectsRegistry(NativeTypeIntrospector.INSTANCE);
        NativeObject testObject = nor.get(TestObject.class);
        assertEquals(TestObject.class, testObject.type());
        assertEquals(0, testObject.typeId());
        assertFalse(testObject.isStruct());
        assertEquals(88, testObject.size());
        assertEquals(8, testObject.alignment());

        Object expected = new String[]{"aByte", "aShort", "anInt", "aLong", "aFloat", "aDouble", "aBoolean", "anEnum",
                "aFloatArray", "aStruct", "objectWithArrays", "aTestObject"};
        Object actual = testObject.fields().stream().map(NativeObject.NativeField::name).toArray(String[]::new);
        assertArrayEquals((String[]) expected, (String[]) actual);

        expected = new int[]{8, 10, 12, 16, 24, 32, 40, 44, 48, 56, 72, 80};
        actual = testObject.fields().stream().mapToInt(NativeObject.NativeField::offset).toArray();
        assertArrayEquals((int[]) expected, (int[]) actual);
        expected = new int[]{1, 2, 4, 8, 4, 8, 1, 4, 8, 12, 8, 8};
        actual = testObject.fields().stream().mapToInt(NativeObject.NativeField::size).toArray();
        assertArrayEquals((int[]) expected, (int[]) actual);

        assertTrue(nor.contains(TestEnum.class));
        NativeObject testEnum = nor.get(TestEnum.class);
        assertEquals(1, testEnum.typeId());
        assertEquals(4, testEnum.size());
        assertEquals(4, testEnum.alignment());


        assertTrue(nor.contains(TestStruct.class)); // TestStruct was required to compute size
        NativeObject testStruct = nor.get(TestStruct.class);
        assertEquals(TestStruct.class, testStruct.type());
        assertEquals(2, testStruct.typeId()); // TestEnum
        assertTrue(testStruct.isStruct());
        assertEquals(12, testStruct.size());
        assertEquals(4, testStruct.alignment());

        assertTrue(nor.contains(TestNativeEnum.class));
        NativeObject testNativeEnum = nor.get(TestNativeEnum.class);
        assertEquals(3, testNativeEnum.typeId());
        assertEquals(4, testNativeEnum.size());
        assertEquals(4, testNativeEnum.alignment());

        assertTrue(nor.contains(TestObjectWithArrays.class));
        NativeObject toa = nor.get(TestObjectWithArrays.class);
        assertEquals(4, toa.typeId());
        assertEquals(88, toa.size());
        assertEquals(8, toa.alignment());

        expected = new String[]{"aByteArray", "aShortArray", "anIntArray", "aLongArray", "aFloatArray", "aDoubleArray",
                "aBooleanArray", "anEnumArray", "aStructArray", "aReferenceArray"};
        actual = toa.fields().stream().map(NativeObject.NativeField::name).toArray(String[]::new);
        assertArrayEquals((String[]) expected, (String[]) actual);
    }

    @Test
    void getWithTypeIdSupplier() {
        Map<Class<?>, Integer> typeIds = Map.of(TestObject.class, 42, TestStruct.class, 43, TestObjectWithArrays.class, 44, TestNativeEnum.class, 45, TestEnum.class, 1);
        NativeObjectsRegistry nor = new NativeObjectsRegistry(NativeTypeIntrospector.INSTANCE, typeIds::get);
        assertEquals(42, nor.get(TestObject.class).typeId());
        assertEquals(43, nor.get(TestStruct.class).typeId());
        assertEquals(44, nor.get(TestObjectWithArrays.class).typeId());
        assertEquals(45, nor.get(TestNativeEnum.class).typeId());
        assertEquals(1, nor.get(TestEnum.class).typeId());
    }


    @Test
    void add() {
        NativeObjectsRegistry nor = new NativeObjectsRegistry();
        NativeObject nativeEnum = new NativeObject(TestEnum.class, 42, 4, 4);
        nor.add(nativeEnum);
        assertTrue(nor.contains(42));
        assertTrue(nor.contains(TestEnum.class));
        assertSame(nativeEnum, nor.get(42));
        assertSame(nativeEnum, nor.get(TestEnum.class));
    }

    @Test
    void unsupportedType() {
        NativeObjectsRegistry nor = new NativeObjectsRegistry();
        assertThrows(IllegalArgumentException.class, () -> nor.get(int.class));
        assertThrows(IllegalArgumentException.class, () -> nor.get(float[].class));
        assertThrows(IllegalArgumentException.class, () -> nor.get(TestObject[].class));
        assertThrows(IllegalArgumentException.class, () -> nor.get(ObjectWithUnsupportedPrimitive.class));
    }

    public static final class ObjectWithUnsupportedPrimitive {
        @NativeOrder(0)
        public char aChar;
    }
}