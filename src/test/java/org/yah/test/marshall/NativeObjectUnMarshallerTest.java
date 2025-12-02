package org.yah.test.marshall;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.junit.jupiter.api.Test;
import org.yah.test.marshall.bytebuffer.ByteBufferAllocation;
import org.yah.test.marshall.foreign.MemorySegmentAllocation;
import org.yah.test.marshall.jna.JnaAllocation;

import java.lang.reflect.Array;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.*;

class NativeObjectUnMarshallerTest extends AbstractNativeTest {

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

    @Test
    void testMarshalObjectMemorySegment() {
        testMarshalObject(MemorySegmentAllocation::new);
    }

    @Test
    void testCallNativeJNA() {
        testCallNative(JnaAllocation::new);
    }

    @Test
    void testCallNativeDirectByteBuffer() {
        testCallNative(ByteBufferAllocation::createDirect);
    }

    @Test
    void testCallNativeMemorySegment() {
        testCallNative(MemorySegmentAllocation::new);
    }


    public interface SandboxNativeInterface extends Library {
        void printTestObject(Pointer testObject, Pointer dst, int maxLength);

        void printTestObjectWithArrays(Pointer testObject, int array, int length, Pointer dst, int maxLength);

        static SandboxNativeInterface load() {
            return Native.load("sandbox-native", SandboxNativeInterface.class);
        }
    }

    private void testMarshalObject(MemoryAllocator<?> allocator) {
        Random random = new Random(12345);
        TestObjects.TestObject root = createTestObject(random);
        TestObjects.TestObject dependency0 = createTestObject(random);
        TestObjects.TestObject dependency1 = createTestObject(random);

        root.aTestObject = dependency0;
        root.objectWithArrays = createTestObjectWithArrays(random);
        root.objectWithArrays.aReferenceArray = new Object[]{dependency0};

        dependency0.objectWithArrays = createTestObjectWithArrays(random);
        dependency0.objectWithArrays.aReferenceArray = new Object[]{dependency0.objectWithArrays.aDoubleArray, dependency1};

        dependency1.objectWithArrays = root.objectWithArrays;

        NativeObjectsRegistry objectsRegistry = new NativeObjectsRegistry(NativeTypeIntrospector.INSTANCE);
        NativeObjectMarshaller marshaller = new NativeObjectMarshaller(objectsRegistry);
        try (NativeInstances<?> nativeInstances = marshaller.marshall(root, allocator, null)) {
            TestObjects.TestObject expectedRoot = cloneAndClean(root);
            TestObjects.TestObject expectedDependency0 = cloneAndClean(dependency0);
            TestObjects.TestObject expectedDependency1 = cloneAndClean(dependency1);

            TestObjects.TestObjectWithArrays expectedRootObjectWithArray = cloneAndClean(expectedRoot.objectWithArrays);
            TestObjects.TestObjectWithArrays expectedDependency0ObjectWithArray = cloneAndClean(expectedDependency0.objectWithArrays);
            NativeObjectUnmarshaller unmarshaller = new NativeObjectUnmarshaller(objectsRegistry);
            unmarshaller.unmarshall(nativeInstances);

            assertTestObject(expectedRoot, root);
            assertTestObjectWithArrays(expectedRootObjectWithArray, root.objectWithArrays);

            assertTestObject(expectedDependency0, dependency0);
            assertTestObjectWithArrays(expectedDependency0ObjectWithArray, dependency0.objectWithArrays);

            assertTestObject(expectedDependency1, dependency1);
        }
    }

    private void testCallNative(MemoryAllocator<?> allocator) {
        if (System.getProperty("jna.library.path") == null) {
            String path = Paths.get("sandbox-native\\x64\\Release").toAbsolutePath().toString();
            System.setProperty("jna.library.path", path);
        }
        SandboxNativeInterface nativeInterface = SandboxNativeInterface.load();
        Random random = new Random(12345);
        TestObjects.TestObject o = createTestObject(random);
        o.objectWithArrays = createTestObjectWithArrays(random);
        o.objectWithArrays.aReferenceArray = new Object[] {o, null};


        NativeObjectsRegistry objectsRegistry = new NativeObjectsRegistry(NativeTypeIntrospector.INSTANCE);
        NativeObjectMarshaller marshaller = new NativeObjectMarshaller(objectsRegistry);

        int toTypeId = objectsRegistry.get(TestObjects.TestObject.class).typeId(), toaTypeId = objectsRegistry.get(TestObjects.TestObjectWithArrays.class).typeId();
        String formatedStruct = String.format(Locale.ENGLISH, "%d,%f,%d", o.aStruct.anInt, o.aStruct.aFloat, o.aStruct.aNativeEnum.nativeValue());
        String expected = String.format(Locale.ENGLISH, "%d,%d,%d,%d,%d,%f,%f,%d,%d,{%s},{%d}", toTypeId, o.aByte, o.aShort, o.anInt, o.aLong,
                o.aFloat, o.aDouble, o.aBoolean ? 1 : 0, o.anEnum.ordinal(), formatedStruct, toaTypeId);

        int maxLength = 16 * 1024;
        try (NativeInstances<?> nativeInstances = marshaller.marshall(o, allocator, null);
             Memory dstPointer = new Memory(maxLength + 1)) {
            Pointer testObjectPointer = new Pointer(nativeInstances.allocation().address());
            nativeInterface.printTestObject(testObjectPointer, dstPointer, maxLength);
            assertEquals(expected, dstPointer.getString(0));

            nativeInterface.printTestObjectWithArrays(testObjectPointer, 0, o.objectWithArrays.aByteArray.length, dstPointer, maxLength);
            assertEquals(formatArray(o.objectWithArrays.aByteArray, "%d"), dstPointer.getString(0));

            nativeInterface.printTestObjectWithArrays(testObjectPointer, 1, o.objectWithArrays.aShortArray.length, dstPointer, maxLength);
            assertEquals(formatArray(o.objectWithArrays.aShortArray, "%d"), dstPointer.getString(0));

            nativeInterface.printTestObjectWithArrays(testObjectPointer, 2, o.objectWithArrays.anIntArray.length, dstPointer, maxLength);
            assertEquals(formatArray(o.objectWithArrays.anIntArray, "%d"), dstPointer.getString(0));

            nativeInterface.printTestObjectWithArrays(testObjectPointer, 3, o.objectWithArrays.aLongArray.length, dstPointer, maxLength);
            assertEquals(formatArray(o.objectWithArrays.aLongArray, "%d"), dstPointer.getString(0));

            nativeInterface.printTestObjectWithArrays(testObjectPointer, 4, o.objectWithArrays.aFloatArray.length, dstPointer, maxLength);
            assertEquals(formatArray(o.objectWithArrays.aFloatArray, "%f"), dstPointer.getString(0));

            nativeInterface.printTestObjectWithArrays(testObjectPointer, 5, o.objectWithArrays.aDoubleArray.length, dstPointer, maxLength);
            assertEquals(formatArray(o.objectWithArrays.aDoubleArray, "%f"), dstPointer.getString(0));

            int[] tmp = new int[o.objectWithArrays.aBooleanArray.length];
            for (int i = 0; i < o.objectWithArrays.aBooleanArray.length; i++)
                tmp[i] = o.objectWithArrays.aBooleanArray[i] ? 1 : 0;
            nativeInterface.printTestObjectWithArrays(testObjectPointer, 6, o.objectWithArrays.aBooleanArray.length, dstPointer, maxLength);
            assertEquals(formatArray(tmp, "%d"), dstPointer.getString(0));

            nativeInterface.printTestObjectWithArrays(testObjectPointer, 7, o.objectWithArrays.anEnumArray.length, dstPointer, maxLength);
            assertEquals(formatArray(Arrays.stream(o.objectWithArrays.anEnumArray).mapToInt(TestObjects.TestEnum::ordinal).toArray(), "%d"),
                    dstPointer.getString(0));

            if (o.objectWithArrays.aReferenceArray != null) {
                nativeInterface.printTestObjectWithArrays(testObjectPointer, 8, o.objectWithArrays.aReferenceArray.length, dstPointer, maxLength);
                assertEquals(formatArray(Arrays.stream(o.objectWithArrays.aReferenceArray)
                        .map(r -> Long.toUnsignedString(nativeInstances.addressOf(r))).toArray(), "%s"), dstPointer.getString(0));
            }
        }
    }

    @Test
    void testMarshallWithParent() {
        Random random = new Random(12345);
        TestObjects.TestObject o1 = createTestObject(random);
        TestObjects.TestObject o2 = createTestObject(random);
        o2.aTestObject = o1;

        NativeObjectsRegistry objectsRegistry = new NativeObjectsRegistry(NativeTypeIntrospector.INSTANCE);
        NativeObjectMarshaller marshaller = new NativeObjectMarshaller(objectsRegistry);
        try (NativeInstances<?> nativeInstances = marshaller.marshall(o1, JnaAllocation::new, null)) {
            try (NativeInstances<?> childNativeInstances = marshaller.marshall(o2, JnaAllocation::new, nativeInstances)) {
                assertTrue(childNativeInstances.contains(o1));
                assertTrue(childNativeInstances.contains(o2));
                assertTrue(nativeInstances.contains(o1));
                assertFalse(nativeInstances.contains(o2));
                assertSame(o1, childNativeInstances.instanceAtAddress(nativeInstances.allocation().address()));
                assertSame(o2, childNativeInstances.instanceAtAddress(childNativeInstances.allocation().address()));
                assertNull(nativeInstances.instanceAtAddress(childNativeInstances.allocation().address()));

                // ensure parent is protected until child is closed
                assertThrows(IllegalStateException.class, nativeInstances::close);
            }
        }
    }

    private static String formatArray(Object array, String format) {
        int length = Array.getLength(array);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format(Locale.ENGLISH, format, Array.get(array, i)));
            if (i < length - 1) sb.append(",");
        }
        return sb.toString();
    }

    protected void assertTestObject(TestObjects.TestObject expected, TestObjects.TestObject actual) {
        super.assertTestObject(expected, actual);
        assertSame(expected.aTestObject, actual.aTestObject);
        assertSame(expected.objectWithArrays, actual.objectWithArrays);
    }

    @Override
    protected void assertTestObjectWithArrays(TestObjects.TestObjectWithArrays expected, TestObjects.TestObjectWithArrays actual) {
        super.assertTestObjectWithArrays(expected, actual);
        assertArrayEquals(expected.aReferenceArray, actual.aReferenceArray);
    }

    private static TestObjects.TestObject cloneAndClean(TestObjects.TestObject obj) {
        TestObjects.TestObject res = new TestObjects.TestObject();
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
        obj.anEnum = TestObjects.TestEnum.A;

        obj.objectWithArrays = null;
        obj.aTestObject = null;

        return res;
    }

    private static TestObjects.TestObjectWithArrays cloneAndClean(TestObjects.TestObjectWithArrays obj) {
        TestObjects.TestObjectWithArrays res = new TestObjects.TestObjectWithArrays();
        res.aByteArray = cloneAndClean(obj.aByteArray, byte[]::new, Arrays::fill, (byte) 0);
        res.aShortArray = cloneAndClean(obj.aShortArray, short[]::new, Arrays::fill, (short) 0);
        res.anIntArray = cloneAndClean(obj.anIntArray, int[]::new, Arrays::fill, 0);
        res.aLongArray = cloneAndClean(obj.aLongArray, long[]::new, Arrays::fill, 0L);
        res.aFloatArray = cloneAndClean(obj.aFloatArray, float[]::new, Arrays::fill, 0f);
        res.aDoubleArray = cloneAndClean(obj.aDoubleArray, double[]::new, Arrays::fill, 0.0);
        res.aBooleanArray = cloneAndClean(obj.aBooleanArray, boolean[]::new, Arrays::fill, false);
        res.anEnumArray = cloneAndClean(obj.anEnumArray, TestObjects.TestEnum[]::new, Arrays::fill, TestObjects.TestEnum.A);
        res.aStructArray = new TestObjects.TestStruct[obj.aStructArray.length];
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

    private static TestObjects.TestStruct cloneAndClean(TestObjects.TestStruct origin) {
        TestObjects.TestStruct dst = new TestObjects.TestStruct();
        dst.anInt = origin.anInt;
        dst.aFloat = origin.aFloat;
        dst.aNativeEnum = origin.aNativeEnum;

        origin.anInt = 0;
        origin.aFloat = 0;
        origin.aNativeEnum = TestObjects.TestNativeEnum.A;

        return dst;
    }



}