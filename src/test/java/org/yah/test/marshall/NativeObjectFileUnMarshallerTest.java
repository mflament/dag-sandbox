package org.yah.test.marshall;

import org.junit.jupiter.api.Test;
import org.yah.test.marshall.NativeObjectFileUnmarshaller.NativeObjectsFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.yah.test.marshall.TestObjects.TestObject;

class NativeObjectFileUnMarshallerTest extends AbstractMarshallerTest {
    @Test
    void testMarshalToFile() throws IOException {
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
        NativeObjectFileMarshaller marshaller = new NativeObjectFileMarshaller(objectsRegistry);
        Path file = Paths.get("target/test_object.bin");
        marshaller.marshall(file, root);

        NativeObjectFileUnmarshaller unmarshaller = new NativeObjectFileUnmarshaller(NativeTypeIntrospector.INSTANCE, null);
        try (NativeObjectsFile nativeObjectsFile = unmarshaller.loadNativeObjectsFile(file)) {
            TestObject actual = nativeObjectsFile.unmarshall(TestObject.class);

            assertTestObject(root, actual);

            assertTestObject(dependency0, root.aTestObject);
            TestObject actualDependency0 = actual.aTestObject;

            assertSame(actualDependency0, actual.objectWithArrays.aReferenceArray[0]);

            assertTestObjectWithArrays(dependency0.objectWithArrays, actualDependency0.objectWithArrays);
            assertSame(actualDependency0.objectWithArrays.aDoubleArray, actualDependency0.objectWithArrays.aReferenceArray[0]);

            TestObject actualDependency1 = assertInstanceOf(TestObject.class, actualDependency0.objectWithArrays.aReferenceArray[1]);
            assertTestObject(dependency1, actualDependency1);
        }
    }
}