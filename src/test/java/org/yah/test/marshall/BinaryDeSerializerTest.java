package org.yah.test.marshall;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.junit.jupiter.api.BeforeAll;

class BinaryDeSerializerTest {

    public static final double MB = (1024 * 1024.0);

    public interface Stdlib extends Library {
        int memcmp(Pointer a, Pointer b, long size);

        static Stdlib load() {
            return Native.load(Stdlib.class);
        }
    }

    private static Stdlib stdlib;

    @BeforeAll
    public static void setupOnce() {
        stdlib = Stdlib.load();
    }

//    private static final class ObjectComparator {
//        private final IdentityHashMap<Object, Boolean> compared = new IdentityHashMap<>();
//        private final ReflectionPath path = new ReflectionPath();
//
//        private void compareInstance(Object expected, Object actual) {
//            if (compared.putIfAbsent(expected, Boolean.TRUE) != null)
//                return;
//            if (expected == null) {
//                assertNull(actual, this::createMessage);
//                return;
//            }
//            assertNotNull(actual, this::createMessage);
//            assertEquals(expected.getClass(), actual.getClass(), createMessage(".class"));
//            Class<?> type = expected.getClass();
//            if (BinarySerializer.isPrimitiveObject(type) || type == String.class || type.isEnum())
//                assertEquals(expected, actual, this::createMessage);
//            else if (type.isArray())
//                compareArray(expected, actual);
//            else if (ResidentTensor.class.isAssignableFrom(type))
//                compareResidentTensor((ResidentTensor) expected, (ResidentTensor) actual);
//            else
//                compareObject(expected, actual);
//        }
//
//        private void compareArray(Object expected, Object actual) {
//            int length = Array.getLength(expected);
//            assertEquals(length, Array.getLength(actual), createMessage(".length"));
//            Class<?> componentType = expected.getClass().componentType();
//            if (componentType.isPrimitive())
//                comparePrimitiveArray(expected, actual);
//            else {
//                for (int i = 0; i < length; i++) {
//                    path.push(i);
//                    Object expectedElement = Array.get(expected, i);
//                    Object actualElement = Array.get(actual, i);
//                    compareInstance(expectedElement, actualElement);
//                    path.pop();
//                }
//            }
//        }
//
//        private void comparePrimitiveArray(Object expected, Object actual) {
//            if (expected instanceof byte[] a) assertArrayEquals(a, (byte[]) actual, this::createMessage);
//            else if (expected instanceof short[] a) assertArrayEquals(a, (short[]) actual, this::createMessage);
//            else if (expected instanceof int[] a) assertArrayEquals(a, (int[]) actual, this::createMessage);
//            else if (expected instanceof long[] a) assertArrayEquals(a, (long[]) actual, this::createMessage);
//            else if (expected instanceof float[] a) assertArrayEquals(a, (float[]) actual, this::createMessage);
//            else if (expected instanceof double[] a) assertArrayEquals(a, (double[]) actual, this::createMessage);
//            else if (expected instanceof boolean[] a) assertArrayEquals(a, (boolean[]) actual, this::createMessage);
//            else assertArrayEquals((char[]) expected, (char[]) actual, this::createMessage);
//        }
//
//        private void compareObject(Object expected, Object actual) {
//            List<Field> fields = BinarySerializer.getFields(expected.getClass());
//            path.push(expected);
//            for (Field field : fields) {
//                path.push(field);
//                Object expectedValue, actualValue;
//                try {
//                    expectedValue = field.get(expected);
//                    actualValue = field.get(actual);
//                } catch (IllegalAccessException e) {
//                    throw new RuntimeException(e);
//                }
//                compareInstance(expectedValue, actualValue);
//                path.pop();
//            }
//            path.pop();
//        }
//
//        private <T extends ResidentTensor> void compareResidentTensor(T expected, T actual) {
//            if (expected instanceof ResidentMatrix me && actual instanceof ResidentMatrix ma) {
//                assertEquals(me.dimX, ma.dimX, createMessage(".dimX"));
//                assertEquals(me.dimY, ma.dimY, createMessage(".dimY"));
//            } else if (expected instanceof ResidentCube ce && actual instanceof ResidentCube ca) {
//                assertEquals(ce.dimX, ca.dimX, createMessage(".dimX"));
//                assertEquals(ce.dimY, ca.dimY, createMessage(".dimY"));
//                assertEquals(ce.dimZ, ca.dimZ, createMessage(".dimZ"));
//            } else
//                throw new IllegalArgumentException("Unsupported tensor " + expected.getClass().getTypeName());
//            long size = expected.size();
//            assertEquals(size, actual.size(), createMessage(".size"));
//            Pointer expectedPointer = expected.pointer();
//            Pointer actualPointer = actual.pointer();
//            if (expectedPointer == null) {
//                assertNull(actualPointer, createMessage(".pointer"));
//                return;
//            }
//            assertNotNull(actualPointer, createMessage(".pointer"));
//            if (stdlib.memcmp(expectedPointer, actualPointer, size) != 0) {
//                fail(createMessage(".pointer"));
//            }
//        }
//
//        private void assertBytesEquals(byte[] expectedBuffer, byte[] actualBuffer, int length) {
//            for (int i = 0; i < length; i++) {
//                assertEquals(expectedBuffer[i], actualBuffer[i], createMessage(".pointer[" + i + "]"));
//            }
//        }
//
//        private String createMessage() {
//            return path.toString();
//        }
//
//        private Supplier<String> createMessage(String suffix) {
//            return () -> path + suffix;
//        }
//    }

}