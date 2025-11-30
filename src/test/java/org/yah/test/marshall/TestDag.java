package org.yah.test.marshall;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class TestDag {

    public static SimulationNode[] createSimulationNodes() {
        Random random = new Random(54321);
        List<SimulationNode> nodes = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            createSimulationNode(random, nodes);
        }
        return nodes.toArray(SimulationNode[]::new);
    }

    private static void createSimulationNode(Random random, List<SimulationNode> nodes) {
        SimulationNode node = new SimulationNode();
        node._key = createSimulationNodeKey(random);
        node._dependencies = createSimulationNodeDependencies(random, nodes);
        node._result = createISimulatable(random);
        nodes.add(node);
    }

    private static SimulationNode[] createSimulationNodeDependencies(Random random, List<SimulationNode> nodes) {
        if (nodes.isEmpty())
            return new SimulationNode[0];

        return createArray(() -> Math.min(nodes.size(), random.nextInt(0, 3)), SimulationNode[].class, () -> nodes.get(random.nextInt(nodes.size())));
    }

    private static SimulationNodeKey createSimulationNodeKey(Random random) {
        SimulationNodeKey key = new SimulationNodeKey();
        key.p = "node_" + random.nextInt();
        return key;
    }

    private static ISimulatable createISimulatable(Random random) {
        SimulatableA a = new SimulatableA();
        a.aByte = (byte) random.nextInt(0xFF);
        a.aShort = (short) random.nextInt(0xFFFF);
        a.anInt = random.nextInt();
        a.aLong = random.nextLong();
        a.aFloat = random.nextFloat();
        a.aDouble = random.nextDouble();
        a.aBoolean = random.nextBoolean();
        a.aChar = (char) random.nextInt();
        a.aTestEnum = randomEnum(random, TestEnum.class);
        a.aString = randomString(random);
        a.aMatrix = createResidentMatrixFloat(random);
        a.aCube = createResidentCubeDouble(random);
        a.withArrays = creatWithArrays(random);
        return a;
    }

    private static WithArrays creatWithArrays(Random random) {
        WithArrays wa = new WithArrays();
        wa.aByteArray = createArray(random, byte[].class, () -> (byte) random.nextInt());
        wa.aShortArray = createArray(random, short[].class, () -> (short) random.nextInt());
        wa.anIntArray = createArray(random, int[].class, random::nextInt);
        wa.aLongArray = createArray(random, long[].class, random::nextLong);
        wa.aFloatArray = createArray(random, float[].class, random::nextFloat);
        wa.aDoubleArray = createArray(random, double[].class, random::nextDouble);
        wa.aBooleanArray = createArray(random, boolean[].class, random::nextBoolean);
        wa.aCharArray = createArray(random, char[].class, () -> (char) random.nextInt(32, 256));
        wa.aTestEnumArray = createArray(random, TestEnum[].class, () -> randomEnum(random, TestEnum.class));
        wa.aStringArray = createArray(random, String[].class, () -> randomString(random));
        wa.aMatrixArray = createArray(random, ResidentMatrixFloat[].class, () -> createResidentMatrixFloat(random));
        wa.aCubeArray = createArray(random, ResidentCubeDouble[].class, () -> createResidentCubeDouble(random));
        wa.moreTensorArray = createArray(random, MoreTensor[].class, () -> createMoreTensor(random));
        return wa;
    }

    private static MoreTensor createMoreTensor(Random random) {
        MoreTensor mt = new MoreTensor();
        mt.aCube = createResidentCubeFloat(random);
        mt.aMatrix = createResidentMatrixDouble(random);
        return mt;
    }

    @SuppressWarnings("SameParameterValue")
    private static <T extends Enum<?>> T randomEnum(Random random, Class<T> type) {
        T[] constants = type.getEnumConstants();
        int ordinal = random.nextInt(constants.length + 1) - 1;
        if (ordinal < 0)
            return null;
        return constants[ordinal];
    }

    private static <T> T createArray(Random random, Class<T> type, Supplier<Object> next) {
        return createArray(() -> random.nextInt(0, 5), type, next);
    }

    @SuppressWarnings("unchecked")
    private static <T> T createArray(IntSupplier lengthSupplier, Class<T> type, Supplier<Object> next) {
        int length = lengthSupplier.getAsInt();
        Object array = Array.newInstance(type.componentType(), length);
        for (int i = 0; i < length; i++) {
            Array.set(array, i, next.get());
        }
        return (T) array;
    }

    private static ResidentMatrixFloat createResidentMatrixFloat(Random random) {
        int dimX = random.nextInt(1, 500);
        int dimY = random.nextInt(1, 200);
        Pointer pointer = createPointer(random, Float.BYTES, dimX, dimY);
        return new ResidentMatrixFloat(pointer, dimX, dimY);
    }

    private static ResidentCubeDouble createResidentCubeDouble(Random random) {
        int dimX = random.nextInt(1, 300);
        int dimY = random.nextInt(1, 200);
        int dimZ = random.nextInt(1, 100);
        Pointer pointer = createPointer(random, Double.BYTES, dimX, dimY, dimZ);
        return new ResidentCubeDouble(pointer, dimX, dimY, dimZ);
    }

    private static ResidentMatrixDouble createResidentMatrixDouble(Random random) {
        int dimX = random.nextInt(1, 100);
        int dimY = random.nextInt(1, 100);
        Pointer pointer = createPointer(random, Double.BYTES, dimX, dimY);
        return new ResidentMatrixDouble(pointer, dimX, dimY);
    }

    private static ResidentCubeFloat createResidentCubeFloat(Random random) {
        int dimX = random.nextInt(1, 100);
        int dimY = random.nextInt(1, 100);
        int dimZ = random.nextInt(1, 50);
        Pointer pointer = createPointer(random, Float.BYTES, dimX, dimY, dimZ);
        return new ResidentCubeFloat(pointer, dimX, dimY, dimZ);
    }

    private static Pointer createPointer(Random random, int componentSize, int... dims) {
        int size = componentSize;
        for (int dim : dims) size *= dim;
        Memory memory = new Memory(size);
        byte[] bytes = new byte[size];
        random.nextBytes(bytes);
        memory.write(0, bytes, 0, bytes.length);
        return memory;
    }

    private static String randomString(Random random) {
        int length = random.nextInt(130);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = (char) random.nextInt(32, 256);
            sb.append(c);
        }
        return sb.toString();
    }

    public static class SimulationNode {
        public SimulationNodeKey _key;
        public ISimulatable _result;
        public SimulationNode[] _dependencies;
    }

    public static class SimulationNodeKey {
        public Object p;
    }

    public interface ISimulatable {
    }

    public enum TestEnum {
        A, B, C, D, E, F, G
    }

    public static class SimulatableA implements ISimulatable {
        public byte aByte;
        public short aShort;
        public int anInt;
        public long aLong;
        public float aFloat;
        public double aDouble;
        public boolean aBoolean;
        public char aChar;

        public TestEnum aTestEnum;

        public String aString;

        public ResidentMatrixFloat aMatrix;
        public ResidentCubeDouble aCube;

        public WithArrays withArrays;
    }

    public static class WithArrays {
        public byte[] aByteArray;
        public short[] aShortArray;
        public int[] anIntArray;
        public long[] aLongArray;
        public float[] aFloatArray;
        public double[] aDoubleArray;
        public boolean[] aBooleanArray;
        public char[] aCharArray;
        public TestEnum[] aTestEnumArray;
        public String[] aStringArray;
        public ResidentMatrixFloat[] aMatrixArray;
        public ResidentCubeDouble[] aCubeArray;
        public MoreTensor[] moreTensorArray;
    }

    public static class MoreTensor {
        public ResidentMatrixDouble aMatrix;
        public ResidentCubeFloat aCube;
    }

    public static abstract class ResidentTensor {
        @Nullable
        protected final Pointer pointer;

        public ResidentTensor(@Nullable Pointer pointer) {
            this.pointer = pointer;
        }

        public Pointer pointer() {
            return pointer;
        }

        abstract long size();
    }

    public static abstract class ResidentMatrix extends ResidentTensor {
        protected final int dimX, dimY;

        public ResidentMatrix(@Nullable Pointer pointer, int dimX, int dimY) {
            super(pointer);
            this.dimX = dimX;
            this.dimY = dimY;
        }
    }

    public static final class ResidentMatrixFloat extends ResidentMatrix {
        public ResidentMatrixFloat(@Nullable Pointer pointer, int dimX, int dimY) {
            super(pointer, dimX, dimY);
        }

        @Override
        long size() {
            return dimX * dimY * (long) Float.BYTES;
        }
    }


    public static final class ResidentMatrixDouble extends ResidentMatrix {
        public ResidentMatrixDouble(@Nullable Pointer pointer, int dimX, int dimY) {
            super(pointer, dimX, dimY);
        }

        @Override
        long size() {
            return dimX * dimY * (long) Double.BYTES;
        }
    }

    public static abstract class ResidentCube extends ResidentTensor {
        protected final int dimX, dimY, dimZ;

        public ResidentCube(@Nullable Pointer pointer, int dimX, int dimY, int dimZ) {
            super(pointer);
            this.dimX = dimX;
            this.dimY = dimY;
            this.dimZ = dimZ;
        }
    }

    public static final class ResidentCubeFloat extends ResidentCube {
        public ResidentCubeFloat(@Nullable Pointer pointer, int dimX, int dimY, int dimZ) {
            super(pointer, dimX, dimY, dimZ);
        }

        @Override
        long size() {
            return dimX * dimY * dimZ * (long) Float.BYTES;
        }
    }


    public static final class ResidentCubeDouble extends ResidentCube {
        public ResidentCubeDouble(@Nullable Pointer pointer, int dimX, int dimY, int dimZ) {
            super(pointer, dimX, dimY, dimZ);
        }

        @Override
        long size() {
            return dimX * dimY * dimZ * (long) Double.BYTES;
        }
    }

}
