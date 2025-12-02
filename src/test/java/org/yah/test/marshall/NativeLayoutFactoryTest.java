package org.yah.test.marshall;

import org.junit.jupiter.api.Test;
import org.yah.test.marshall.NativeInstancesLayout.LayoutEntry;

import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.*;
import static org.yah.test.marshall.NativeInstancesLayout.align;
import static org.yah.test.marshall.TestObjects.*;

class NativeLayoutFactoryTest {

    @Test
    void testLayout() {
        NativeObjectsRegistry objectsRegistry = new NativeObjectsRegistry(NativeTypeIntrospector.INSTANCE);
        NativeLayoutFactory layoutFactory = new NativeLayoutFactory(objectsRegistry);

        TestObject to0 = createTestObject(createTestObjectWithArrays());
        TestObject to1 = createTestObject(to0.objectWithArrays);
        TestObject to2 = createTestObject(createTestObjectWithArrays());
        TestObject to3 = createTestObject(null);
        TestObject to4 = createTestObject(null);
        to0.aTestObject = to1;
        to0.objectWithArrays.aReferenceArray = new Object[]{to0, to1, to2};

        to2.objectWithArrays.aReferenceArray = new Object[]{to0, to3};
        to3.aTestObject = to4;

        NativeInstancesLayout layout = layoutFactory.createLayout(to0);
        int testObjectSize = objectsRegistry.get(TestObject.class).size();
        assertEntry(layout, to0, testObjectSize);
        assertEntry(layout, to1, testObjectSize);
        assertEntry(layout, to2, testObjectSize);
        assertEntry(layout, to3, testObjectSize);
        assertEntry(layout, to4, testObjectSize);

        assertEntry(layout, to0.objectWithArrays.aByteArray, 5);
        assertEntry(layout, to0.objectWithArrays.aShortArray, 5 * Short.BYTES);
        assertEntry(layout, to0.objectWithArrays.anIntArray, 5 * Integer.BYTES);
        assertEntry(layout, to0.objectWithArrays.aLongArray, 5 * Long.BYTES);
        assertEntry(layout, to0.objectWithArrays.aFloatArray, 5 * Float.BYTES);
        assertEntry(layout, to0.objectWithArrays.aDoubleArray, 5 * Double.BYTES);
        assertEntry(layout, to0.objectWithArrays.aBooleanArray, 5);
        assertEntry(layout, to0.objectWithArrays.aReferenceArray, 3 * Long.BYTES);

        assertSame(to1, assertContains(layout, to0.aTestObject).instance());
        assertSame(to1.objectWithArrays, assertContains(layout, to1.objectWithArrays).instance());

        assertEntry(layout, to2.objectWithArrays.aReferenceArray, 2 * Long.BYTES);
        assertSame(to4, assertContains(layout, to3.aTestObject).instance());

        assertEntry(layout, to0.objectWithArrays.aByteArray, 5);

        long expectedOffset = 0;
        for (LayoutEntry entry : layout) {
            assertEquals(0, entry.offset() % 8);
            assertEquals(expectedOffset, entry.offset());
            expectedOffset = align(entry.offset() + entry.size(), Long.BYTES);
        }
        assertEquals(expectedOffset, layout.size());

        layoutFactory = new NativeLayoutFactory(objectsRegistry, 1);
        layout = layoutFactory.createLayout(to0, null);
        expectedOffset = 0;
        for (LayoutEntry entry : layout) {
            assertEquals(expectedOffset, entry.offset());
            expectedOffset = entry.offset() + entry.size();
        }
        assertEquals(expectedOffset, layout.size());

    }

    private static LayoutEntry assertContains(NativeInstancesLayout layout, Object instance) {
        LayoutEntry entry = layout.get(instance);
        assertNotNull(entry);
        return entry;
    }

    private void assertEntry(NativeInstancesLayout layout, Object instance, int expectedSize) {
        LayoutEntry entry = layout.get(instance);
        assertNotNull(entry);
        assertSame(entry.instance(), instance);
        assertEquals(expectedSize, entry.size());
    }

    private static TestObject createTestObject(@Nullable TestObjectWithArrays objectWithArrays) {
        TestObject testObject = new TestObject();
        testObject.aFloatArray = new float[10];
        testObject.objectWithArrays = objectWithArrays;
        return testObject;
    }

    private static TestObjectWithArrays createTestObjectWithArrays() {
        TestObjectWithArrays toa = new TestObjectWithArrays();
        toa.aByteArray = new byte[5];
        toa.aShortArray = new short[5];
        toa.anIntArray = new int[5];
        toa.aLongArray = new long[5];
        toa.aFloatArray = new float[5];
        toa.aDoubleArray = new double[5];
        toa.aBooleanArray = new boolean[5];
        toa.anEnumArray = new TestEnum[5];
        toa.aStructArray = new TestStruct[5];
        return toa;
    }
}