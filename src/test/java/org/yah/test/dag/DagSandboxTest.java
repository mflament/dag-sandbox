package org.yah.test.dag;

import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.yah.test.dag.DagSandbox.*;

class DagSandboxTest {

    private static final Random RANDOM = new Random(12345);

    @Test
    void testNode() {
        NodeGroup a = new NodeGroup(A), b = new NodeGroup(B, a), c = new NodeGroup(C, b);
        assertEquals(0, a.depth());
        assertEquals(1, b.depth());
        assertEquals(2, c.depth());

        assertFalse(a.dependsOn(b));
        assertTrue(b.dependsOn(a));
        assertTrue(c.dependsOn(a));
        assertFalse(c.dependsOn(a, false));
    }

    @Test
    void testCreateDag() {
        NodeGroup a = new NodeGroup(A), b = new NodeGroup(B, a), c = new NodeGroup(C, a), d = new NodeGroup(D, b), e = new NodeGroup(E, c);
        NodeGroup[] dag = createDag(shuffle(c, b, d, a, e));
        assertNodeTypes(dag, A, B, C, D, E);
    }

    @Test
    void testNotParallelizable() {
        NodeGroup a = new NodeGroup(A), b = new NodeGroup(B, a), c = new NodeGroup(C, b), d = new NodeGroup(D, c), e = new NodeGroup(E, d);
        NodeGroup[][] pdag = parallelizedDag(shuffle(a, b, c, d, e));
        assertNodeTypes(pdag, types(A), types(B), types(C), types(D), types(E));
    }

    @Test
    void testParallelizableWithoutSupplier() {
        NodeGroup a = new NodeGroup(A), b = new NodeGroup(B, a), c = new NodeGroup(C, a),
                d = new NodeGroup(D, b), e = new NodeGroup(E, c), f = new NodeGroup(F, d, e);
        NodeGroup[][] pdag = parallelizedDag(shuffle(a, b, c, d, e, f));
        assertNodeTypes(pdag, types(A), types(B, C), types(D, E), types(F));
    }

    @Test
    void testParallelizableWithSupplierForC() {
        NodeGroup a = new NodeGroup(A), b = new NodeGroup(B, a), c = new NodeGroup(C, a),
                d = new NodeGroup(D, b), e = new NodeGroup(E, c), f = new NodeGroup(F, d, e);
        List<NodeGroup> nodes = shuffle(a, b, c, d, e, f);
        NodeGroup[][] pdag = parallelizedDag(nodes, DependencySupplier.byType(nodes, Map.of(C, Set.of(B))));
        assertNodeTypes(pdag, types(A), types(B), types(C, D), types(E), types(F));
    }

    @Test
    void testParallelizableWithSupplierForE() {
        NodeGroup a = new NodeGroup(A), b = new NodeGroup(B, a), c = new NodeGroup(C, a),
                d = new NodeGroup(D, b), e = new NodeGroup(E, c), f = new NodeGroup(F, d, e);
        List<NodeGroup> nodes = shuffle(a, b, c, d, e, f);
        NodeGroup[][] pdag = parallelizedDag(nodes, DependencySupplier.byType(nodes, Map.of(E, Set.of(D))));
        assertNodeTypes(pdag, types(A), types(B, C), types(D), types(E), types(F));
    }

    @Test
    void testParallelizableWithMaxNodeCount() {
        NodeGroup a = new NodeGroup(A),
                b = new NodeGroup(B, 3, a), c = new NodeGroup(C, 5, a),
                d = new NodeGroup(D, 5, b), e = new NodeGroup(E, 8, c), // sorted by group size, D < E
                f = new NodeGroup(F, d, e);
        NodeGroup[][] pdag = parallelizedDag(shuffle(a, b, c, d, e, f), 10);
        assertNodeTypes(pdag, types(A), types(B, C), types(D), types(E), types(F));
    }

    private void assertNodeTypes(NodeGroup[] dag, NodeType... expectedTypes) {
        NodeType[] actualTypes = types(dag);
        String message = "expected=" + format(expectedTypes) + ", actual=" + format(actualTypes);
        assertArrayEquals(expectedTypes, actualTypes, message);
    }

    @SuppressWarnings("unchecked")
    private void assertNodeTypes(NodeGroup[][] pdag, NodeType[]... expectedTypes) {
        Set<NodeType>[] actualTypeSets = types(pdag);
        Set<NodeType>[] expectedTypeSets = (Set<NodeType>[]) Arrays.stream(expectedTypes).map(Set::of).toArray(Set[]::new);
        String message = "expected=" + format(expectedTypeSets) + ", actual=" + format(actualTypeSets);
        assertArrayEquals(expectedTypeSets, actualTypeSets, message);
    }

    private static List<NodeGroup> shuffle(NodeGroup... nodes) {
        List<NodeGroup> list = new ArrayList<>(Arrays.asList(nodes));
        Collections.shuffle(list, RANDOM);
        return list;
    }

    @Nonnull
    private static NodeType[] types(NodeGroup[] dag) {
        return Arrays.stream(dag).map(NodeGroup::type).toArray(NodeType[]::new);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    private static Set<NodeType>[] types(NodeGroup[][] pdag) {
        return (Set<NodeType>[]) Arrays.stream(pdag).map(layer -> Set.of(types(layer))).toArray(Set[]::new);
    }

    private static NodeType[] types(NodeType type, NodeType... others) {
        NodeType[] res = new NodeType[1 + others.length];
        res[0] = type;
        System.arraycopy(others, 0, res, 1, others.length);
        return res;
    }

    private static String format(Set<NodeType>[] ptypes) {
        return "[" + Arrays.stream(ptypes).map(set -> Arrays.toString(set.stream().sorted().toArray(NodeType[]::new)))
                .collect(Collectors.joining(", ")) + "]";
    }

    private static String format(NodeType[] types) {
        return Arrays.toString(types);
    }

    private static final NodeType A = new TestNodeType("A");
    private static final NodeType B = new TestNodeType("B");
    private static final NodeType C = new TestNodeType("C");
    private static final NodeType D = new TestNodeType("D");
    private static final NodeType E = new TestNodeType("E");
    private static final NodeType F = new TestNodeType("F");

    private record TestNodeType(String name) implements NodeType {
        @Override
        @Nonnull
        public String toString() {
            return name;
        }

        @Override
        public int compareTo(NodeType o) {
            if (o instanceof TestNodeType tnt)
                return name.compareTo(tnt.name);
            return NodeType.super.compareTo(o);
        }
    }
}