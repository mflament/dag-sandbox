package org.yah.test.marshall;

import org.junit.jupiter.api.Test;
import org.yah.test.marshall.NativeObjectMarshaller.NativeInstances;
import org.yah.test.marshall.bytebuffer.ByteBufferAllocation;
import org.yah.test.marshall.jna.JnaAllocation;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.yah.test.marshall.TestObjects.*;

class NativeObjectUnMarshallerTest extends AbstractMarshallerTest {

    @Test
    void testMarshalObjectByteBuffer() {
        testMarshalObject(ByteBufferAllocation::create);
    }

    @Test
    void testMarshalObjectDirectByteBuffer() {
        testMarshalObject(ByteBufferAllocation::createDirect);
    }

    @Test
    void testMarshalObjectJNA() {
        testMarshalObject(JnaAllocation::new);
    }

    private void testMarshalObject(MemoryAllocator<?> allocator) {
        Random random = new Random(12345);
        TestObject root = createTestObject(random);
        TestObject dependency0 = createTestObject(random);
        TestObject dependency1 = createTestObject(random);

        root.aTestObject = dependency0;
        root.objectWithArrays = createTestObjectWithArrays(random);
        root.objectWithArrays.aReferenceArray = new Object[]{dependency0};

        dependency0.objectWithArrays = createTestObjectWithArrays(random);
        dependency0.objectWithArrays.aReferenceArray = new Object[]{dependency0.objectWithArrays.aDoubleArray, dependency1};

        dependency1.objectWithArrays = root.objectWithArrays;

        NativeObjectsRegistry objectsRegistry = new NativeObjectsRegistry(NativeTypeIntrospector.INSTANCE);
        NativeObjectMarshaller marshaller = new NativeObjectMarshaller(objectsRegistry);
        try (NativeInstances<?> nativeInstances = marshaller.marshall(root, allocator)) {
            TestObject expectedRoot = cloneAndClean(root);
            TestObject expectedDependency0 = cloneAndClean(dependency0);
            TestObject expectedDependency1 = cloneAndClean(dependency1);

            TestObjectWithArrays expectedRootObjectWithArray = cloneAndClean(expectedRoot.objectWithArrays);
            TestObjectWithArrays expectedDependency0ObjectWithArray = cloneAndClean(expectedDependency0.objectWithArrays);
            NativeObjectUnmarshaller unmarshaller = new NativeObjectUnmarshaller(objectsRegistry);
            unmarshaller.unmarshall(nativeInstances);

            assertTestObject(expectedRoot, root);
            assertTestObjectWithArrays(expectedRootObjectWithArray, root.objectWithArrays);

            assertTestObject(expectedDependency0, dependency0);
            assertTestObjectWithArrays(expectedDependency0ObjectWithArray, dependency0.objectWithArrays);

            assertTestObject(expectedDependency1, dependency1);
        }
    }

    protected void assertTestObject(TestObject expected, TestObject actual) {
        super.assertTestObject(expected, actual);
        assertSame(expected.aTestObject, actual.aTestObject);
        assertSame(expected.objectWithArrays, actual.objectWithArrays);
    }

    @Override
    protected void assertTestObjectWithArrays(TestObjectWithArrays expected, TestObjectWithArrays actual) {
        super.assertTestObjectWithArrays(expected, actual);
        assertArrayEquals(expected.aReferenceArray, actual.aReferenceArray);
    }

    private static TestObject cloneAndClean(TestObject obj) {
        TestObject res = new TestObject();
        res.aByte = obj.aByte;
        res.aShort = obj.aShort;
        res.anInt = obj.anInt;
        res.aLong = obj.aLong;
        res.aFloat = obj.aFloat;
        res.aDouble = obj.aDouble;
        res.aBoolean = obj.aBoolean;
        res.anEnum = obj.anEnum;
        res.aStruct = cloneAndClean(obj.aStruct);
        res.aTestObject = obj.aTestObject;
        res.objectWithArrays = obj.objectWithArrays;

        obj.aByte = 0;
        obj.aShort = 0;
        obj.anInt = 0;
        obj.aLong = 0;
        obj.aFloat = 0;
        obj.aDouble = 0;
        obj.aBoolean = false;
        obj.anEnum = TestEnum.A;

        obj.objectWithArrays = null;
        obj.aTestObject = null;

        return res;
    }

    private static TestObjectWithArrays cloneAndClean(TestObjectWithArrays obj) {
        TestObjectWithArrays res = new TestObjectWithArrays();
        res.aByteArray = cloneAndClean(obj.aByteArray, byte[]::new, Arrays::fill, (byte) 0);
        res.aShortArray = cloneAndClean(obj.aShortArray, short[]::new, Arrays::fill, (short) 0);
        res.anIntArray = cloneAndClean(obj.anIntArray, int[]::new, Arrays::fill, 0);
        res.aLongArray = cloneAndClean(obj.aLongArray, long[]::new, Arrays::fill, 0L);
        res.aFloatArray = cloneAndClean(obj.aFloatArray, float[]::new, Arrays::fill, 0f);
        res.aDoubleArray = cloneAndClean(obj.aDoubleArray, double[]::new, Arrays::fill, 0.0);
        res.aBooleanArray = cloneAndClean(obj.aBooleanArray, boolean[]::new, Arrays::fill, false);
        res.anEnumArray = cloneAndClean(obj.anEnumArray, TestEnum[]::new, Arrays::fill, TestEnum.A);
        res.aStructArray = new TestStruct[obj.aStructArray.length];
        for (int i = 0; i < obj.aStructArray.length; i++) res.aStructArray[i] = cloneAndClean(obj.aStructArray[i]);
        if (obj.aReferenceArray != null) {
            res.aReferenceArray = new Object[obj.aReferenceArray.length];
            System.arraycopy(obj.aReferenceArray, 0, res.aReferenceArray, 0, obj.aReferenceArray.length);
            Arrays.fill(obj.aReferenceArray, null);
        }
        return res;
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    private static <A, E> A cloneAndClean(A array, IntFunction<A> constructor, BiConsumer<A, E> fill, E defaultValue) {
        int length = Array.getLength(array);
        A res = constructor.apply(length);
        System.arraycopy(array, 0, res, 0, length);
        fill.accept(array, defaultValue);
        return res;
    }

    private static TestStruct cloneAndClean(TestStruct origin) {
        TestStruct dst = new TestStruct();
        dst.anInt = origin.anInt;
        dst.aFloat = origin.aFloat;
        dst.aNativeEnum = origin.aNativeEnum;

        origin.anInt = 0;
        origin.aFloat = 0;
        origin.aNativeEnum = TestNativeEnum.A;

        return dst;
    }

}