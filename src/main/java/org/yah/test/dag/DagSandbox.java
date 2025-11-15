package org.yah.test.dag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DagSandbox {

    public interface NodeType extends Comparable<NodeType> {
        @Override
        default int compareTo(NodeType o) {
            return getClass().getSimpleName().compareTo(o.getClass().getSimpleName());
        }
    }

    public static class NodeGroup {

        public final NodeType type;

        private final List<NodeGroup> dependencies = new ArrayList<>();

        public final int nodeCount;

        public NodeGroup(NodeType type, int nodeCount, NodeGroup... dependencies) {
            this.type = type;
            this.nodeCount = nodeCount;
            this.dependencies.addAll(Arrays.asList(dependencies));
        }

        public NodeType type() {
            return type;
        }

        public NodeGroup(NodeType type, NodeGroup... dependencies) {
            this(type, 1, dependencies);
        }

        public List<NodeGroup> dependencies() {
            return dependencies;
        }

        public int nodeCount() {
            return nodeCount;
        }

        public int depth() {
            if (dependencies.isEmpty())
                return 0;
            return 1 + dependencies.stream().mapToInt(NodeGroup::depth).max().orElseThrow();
        }

        public boolean dependsOn(NodeGroup other) {
            return dependsOn(other, true);
        }

        public boolean dependsOn(NodeGroup other, boolean transitive) {
            if (dependencies.contains(other))
                return true;
            if (transitive)
                return dependencies.stream().anyMatch(dep -> dep.dependsOn(other, true));
            return false;
        }

        @Override
        public String toString() {
            return type.toString();
        }
    }

    public record ParallelizedLayer(int depth, ArrayList<NodeGroup> nodeGroups) {
        public ParallelizedLayer(int depth) {
            this(depth, new ArrayList<>());
        }

        public NodeGroup[] toArray() {
            return nodeGroups.toArray(NodeGroup[]::new);
        }

        public void add(NodeGroup node) {
            nodeGroups.add(node);
        }

        public int nodeCount() {
            return nodeGroups().stream().mapToInt(NodeGroup::nodeCount).sum();
        }

        @Override
        @Nonnull
        public String toString() {
            return "[" + nodeGroups.stream().map(NodeGroup::toString).collect(Collectors.joining(", ")) + "]";
        }

        public void sortGroups() {
            nodeGroups.sort(Comparator.comparing(NodeGroup::nodeCount));
        }
    }

    @FunctionalInterface
    public interface DependencySupplier extends Function<NodeGroup, Collection<NodeGroup>> {
        Collection<NodeGroup> apply(NodeGroup node);

        static DependencySupplier byType(Collection<NodeGroup> nodes, Map<NodeType, Set<NodeType>> typesDependencies) {
            Map<NodeGroup, List<NodeGroup>> nodeDependencies = new HashMap<>();
            for (NodeGroup node : nodes) {
                List<NodeGroup> dependencies = new LinkedList<>(node.dependencies());
                Collection<NodeType> nodeTypeDependencies = typesDependencies.get(node.type());
                if (nodeTypeDependencies != null) {
                    nodes.stream().filter(n -> nodeTypeDependencies.contains(n.type())).forEach(dependencies::add);
                }
                nodeDependencies.put(node, dependencies);
            }

            return nodeDependencies::get;
        }
    }

    public static NodeGroup[] createDag(Collection<NodeGroup> nodes) {
        List<NodeGroup> layers = new ArrayList<>(nodes);
        layers.sort(Comparator.comparing(NodeGroup::depth).thenComparing(NodeGroup::type));
        return layers.toArray(NodeGroup[]::new);
    }

    public static NodeGroup[][] parallelizedDag(Collection<NodeGroup> nodes) {
        return parallelizedDag(nodes, (DependencySupplier) null);
    }

    public static NodeGroup[][] parallelizedDag(Collection<NodeGroup> nodes, @Nullable DependencySupplier dependencySupplier) {
        if (nodes.isEmpty())
            return new NodeGroup[0][];

        if (dependencySupplier == null)
            dependencySupplier = NodeGroup::dependencies;

        List<ParallelizedLayer> layers = createParallelizedLayers(nodes, dependencySupplier);

        return layers.stream()
                .sorted(Comparator.comparing(ParallelizedLayer::depth))
                .map(ParallelizedLayer::toArray)
                .toArray(NodeGroup[][]::new);
    }

    public static NodeGroup[][] parallelizedDag(Collection<NodeGroup> nodes, @Nullable Integer maxNodesPerLayer) {
        if (nodes.isEmpty())
            return new NodeGroup[0][];

        List<ParallelizedLayer> layers = createParallelizedLayers(nodes, NodeGroup::dependencies);
        if (maxNodesPerLayer != null) {
            List<ParallelizedLayer> cappedLayers = null;
            for (int i = 0; i < layers.size(); i++) {
                ParallelizedLayer layer = layers.get(i);
                if (layer.nodeCount() > maxNodesPerLayer) {
                    if (cappedLayers == null) {
                        cappedLayers = new ArrayList<>();
                        if (i > 0)
                            cappedLayers.addAll(layers.subList(0, i));
                    }

                    ParallelizedLayer cappedLayer = null;
                    layer.sortGroups();
                    for (NodeGroup nodeGroup : layer.nodeGroups()) {
                        if (cappedLayer == null || cappedLayer.nodeCount() + nodeGroup.nodeCount() >= maxNodesPerLayer) {
                            cappedLayer = new ParallelizedLayer(cappedLayers.size());
                            cappedLayer.add(nodeGroup);
                            cappedLayers.add(cappedLayer);
                        } else {
                            cappedLayer.add(nodeGroup);
                        }
                    }
                } else if (cappedLayers != null) {
                    cappedLayers.add(layer);
                }
            }

            if (cappedLayers != null)
                layers = cappedLayers;
        }

        return layers.stream()
                .sorted(Comparator.comparing(ParallelizedLayer::depth))
                .map(ParallelizedLayer::toArray)
                .toArray(NodeGroup[][]::new);
    }

    @Nonnull
    private static List<ParallelizedLayer> createParallelizedLayers(Collection<NodeGroup> nodes, @Nonnull DependencySupplier dependencySupplier) {
        List<ParallelizedLayer> layers = new ArrayList<>();
        for (NodeGroup node : nodes) {
            int depth = getDepth(node, dependencySupplier);

            ParallelizedLayer layer = layers.stream().filter(pl -> pl.depth == depth).findFirst().orElseGet(() -> {
                ParallelizedLayer newLayer = new ParallelizedLayer(depth);
                layers.add(newLayer);
                return newLayer;
            });

            layer.add(node);
        }
        layers.sort(Comparator.comparing(ParallelizedLayer::depth));
        return layers;
    }

    public static int getDepth(NodeGroup node, DependencySupplier dependencySupplier) {
        List<NodeGroup> dependencies = new ArrayList<>(dependencySupplier.apply(node));
        List<NodeGroup> nextDependencies = new ArrayList<>();
        int depth = 0;
        while (!dependencies.isEmpty()) {
            depth++;
            nextDependencies.clear();
            for (NodeGroup dependency : dependencies) {
                nextDependencies.addAll(dependencySupplier.apply(dependency));
            }
            var swap = dependencies;
            dependencies = nextDependencies;
            nextDependencies = swap;
        }
        return depth;
    }

}
