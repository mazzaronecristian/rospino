package org.oristool.rospino2.computingNodes;

import org.oristool.eulero.modeling.stochastictime.StochasticTime;

import java.math.BigDecimal;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

/** A bidirectional weighted graph backed by ordered lists instead of maps. */
public class ListBidirectionalWeightedGraph<N> {
    private final List<Node<N>> nodes = new ArrayList<>();
    private final Set<N> nodesView = Collections.unmodifiableSet(new AbstractSet<>() {
        @Override
        public Iterator<N> iterator() {
            return new Iterator<>() {
                private int index;

                @Override
                public boolean hasNext() {
                    return index < nodes.size();
                }

                @Override
                public N next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return nodes.get(index++).id;
                }
            };
        }

        @Override
        public int size() {
            return nodes.size();
        }

        @Override
        public boolean contains(Object id) {
            return findNodeIndex(id) >= 0;
        }
    });

    /** Adds an isolated node. Existing nodes are left unchanged. */
    public void addNode(N id) {
        Objects.requireNonNull(id, "Node cannot be null");
        if (findNodeIndex(id) < 0) {
            nodes.add(new Node<>(id));
        }
    }

    /**
     * Adds or replaces an undirected edge. Missing endpoints are added
     * automatically.
     *
     * @throws IllegalArgumentException if the weight is null or has a negative LFT
     */
    public void addEdge(N from, N to, StochasticTime weight) {
        Objects.requireNonNull(from, "Source node cannot be null");
        Objects.requireNonNull(to, "Destination node cannot be null");
        if (weight == null) {
            throw new IllegalArgumentException("Edge weight cannot be null");
        }
        BigDecimal lft = Objects.requireNonNull(weight.getLFT(), "Edge weight LFT cannot be null");
        if (lft.signum() < 0) {
            throw new IllegalArgumentException("Edge weight LFT cannot be negative");
        }

        addNode(from);
        addNode(to);
        Node<N> fromNode = nodes.get(findNodeIndex(from));
        Node<N> toNode = nodes.get(findNodeIndex(to));
        setEdge(fromNode, to, weight);
        if (!Objects.equals(from, to)) {
            setEdge(toNode, from, weight);
        }
    }

    /** Returns an unmodifiable view of the nodes in insertion order. */
    public Set<N> nodes() {
        return nodesView;
    }

    /** Alias retained for callers that refer to the node set as a key set. */
    public Set<N> keySet() {
        return nodes();
    }

    public boolean hasNode(N id) {
        return findNodeIndex(id) >= 0;
    }

    public boolean hasEdge(N from, N to) {
        int fromIndex = findNodeIndex(from);
        return fromIndex >= 0 && findEdgeIndex(nodes.get(fromIndex), to) >= 0;
    }

    public Optional<StochasticTime> getWeight(N from, N to) {
        int fromIndex = findNodeIndex(from);
        if (fromIndex < 0) {
            return Optional.empty();
        }
        Node<N> node = nodes.get(fromIndex);
        int edgeIndex = findEdgeIndex(node, to);
        return edgeIndex < 0 ? Optional.empty() : Optional.of(node.edges.get(edgeIndex).weight);
    }

    public int nodeCount() {
        return nodes.size();
    }

    /** Counts undirected edges once, including a self-loop once. */
    public int edgeCount() {
        int count = 0;
        for (int fromIndex = 0; fromIndex < nodes.size(); fromIndex++) {
            for (Edge<N> edge : nodes.get(fromIndex).edges) {
                int toIndex = findNodeIndex(edge.to);
                if (toIndex >= fromIndex) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Finds a minimum-cost path using the sum of edge LFTs.
     *
     * @return the path including both endpoints, or empty if no path exists
     * @throws NoSuchElementException if either endpoint is not in the graph
     */
    public Optional<List<N>> shortestPath(N from, N to) {
        int fromIndex = requireNode(from);
        int toIndex = requireNode(to);
        if (fromIndex == toIndex) {
            return Optional.of(List.of(from));
        }

        List<BigDecimal> distances = new ArrayList<>(Collections.nCopies(nodes.size(), null));
        List<Integer> previous = new ArrayList<>(Collections.nCopies(nodes.size(), -1));
        List<Boolean> settled = new ArrayList<>(Collections.nCopies(nodes.size(), false));
        PriorityQueue<PathCandidate> queue = new PriorityQueue<>(
                Comparator.comparing(PathCandidate::distance).thenComparingLong(PathCandidate::sequence));
        long sequence = 0;
        distances.set(fromIndex, BigDecimal.ZERO);
        queue.add(new PathCandidate(fromIndex, BigDecimal.ZERO, sequence++));

        while (!queue.isEmpty()) {
            PathCandidate current = queue.remove();
            BigDecimal bestKnownDistance = distances.get(current.nodeIndex());
            if (settled.get(current.nodeIndex())
                    || current.distance().compareTo(bestKnownDistance) > 0) {
                continue;
            }
            settled.set(current.nodeIndex(), true);
            if (current.nodeIndex() == toIndex) {
                break;
            }

            for (Edge<N> edge : nodes.get(current.nodeIndex()).edges) {
                int neighborIndex = findNodeIndex(edge.to);
                if (settled.get(neighborIndex)) {
                    continue;
                }
                BigDecimal nextDistance = current.distance().add(edge.weight.getLFT());
                BigDecimal oldDistance = distances.get(neighborIndex);
                if (oldDistance == null || nextDistance.compareTo(oldDistance) < 0) {
                    distances.set(neighborIndex, nextDistance);
                    previous.set(neighborIndex, current.nodeIndex());
                    queue.add(new PathCandidate(neighborIndex, nextDistance, sequence++));
                }
            }
        }

        if (distances.get(toIndex) == null) {
            return Optional.empty();
        }

        LinkedList<N> path = new LinkedList<>();
        for (int index = toIndex; index >= 0; index = previous.get(index)) {
            path.addFirst(nodes.get(index).id);
            if (index == fromIndex) {
                return Optional.of(List.copyOf(path));
            }
        }
        return Optional.empty();
    }

    private void setEdge(Node<N> from, N to, StochasticTime weight) {
        int edgeIndex = findEdgeIndex(from, to);
        Edge<N> edge = new Edge<>(to, weight);
        if (edgeIndex < 0) {
            from.edges.add(edge);
        } else {
            from.edges.set(edgeIndex, edge);
        }
    }

    private int findNodeIndex(Object id) {
        for (int index = 0; index < nodes.size(); index++) {
            if (Objects.equals(nodes.get(index).id, id)) {
                return index;
            }
        }
        return -1;
    }

    private int findEdgeIndex(Node<N> node, Object to) {
        for (int index = 0; index < node.edges.size(); index++) {
            if (Objects.equals(node.edges.get(index).to, to)) {
                return index;
            }
        }
        return -1;
    }

    private int requireNode(N id) {
        int index = findNodeIndex(id);
        if (index < 0) {
            throw new NoSuchElementException("Node not found: " + id);
        }
        return index;
    }

    private static final class Node<T> {
        private final T id;
        private final List<Edge<T>> edges = new ArrayList<>();

        private Node(T id) {
            this.id = id;
        }
    }

    private record Edge<T>(T to, StochasticTime weight) { }

    private record PathCandidate(int nodeIndex, BigDecimal distance, long sequence) { }
}
