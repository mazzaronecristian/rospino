package org.oristool.rospino2.computingNodes;

import org.oristool.eulero.modeling.stochastictime.StochasticTime;

import java.math.BigDecimal;
import java.util.*;

/**
 * A bidirectional weighted graph whose edge costs are measured by the LFT of
 * their {@link StochasticTime} weights.
 *
 * @param <N> the node type
 */
public class BidirectionalWeightedGraph<N> {
    private final Map<N, Map<N, StochasticTime>> adjacency = new LinkedHashMap<>();

    /** Adds an isolated node. Existing nodes are left unchanged. */
    public void addNode(N id) {
        adjacency.computeIfAbsent(Objects.requireNonNull(id, "Node cannot be null"), ignored -> new LinkedHashMap<>());
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
        adjacency.get(from).put(to, weight);
        adjacency.get(to).put(from, weight);
    }

    /** Returns an unmodifiable view of the graph's nodes. */
    public Set<N> nodes() {
        return Collections.unmodifiableSet(adjacency.keySet());
    }

    /** Alias retained for callers that refer to the node set as a key set. */
    public Set<N> keySet() {
        return nodes();
    }

    public boolean hasNode(N id) {
        return adjacency.containsKey(id);
    }

    public boolean hasEdge(N from, N to) {
        return adjacency.containsKey(from) && adjacency.get(from).containsKey(to);
    }

    public Optional<StochasticTime> getWeight(N from, N to) {
        Map<N, StochasticTime> neighbors = adjacency.get(from);
        return neighbors == null ? Optional.empty() : Optional.ofNullable(neighbors.get(to));
    }

    public int nodeCount() {
        return adjacency.size();
    }

    /** Counts undirected edges once. */
    public int edgeCount() {
        int directedEntries = adjacency.values().stream().mapToInt(Map::size).sum();
        int selfLoops = adjacency.entrySet().stream()
                .mapToInt(entry -> entry.getValue().containsKey(entry.getKey()) ? 1 : 0)
                .sum();
        return (directedEntries - selfLoops) / 2 + selfLoops;
    }

    /**
     * Finds a minimum-cost path using the sum of edge LFTs.
     *
     * @return the path including both endpoints, or empty if no path exists
     * @throws NoSuchElementException if either endpoint is not in the graph
     */
    public Optional<List<N>> shortestPath(N from, N to) {
        requireNode(from);
        requireNode(to);
        if (Objects.equals(from, to)) {
            return Optional.of(List.of(from));
        }

        Map<N, BigDecimal> distances = new HashMap<>();
        Map<N, N> previous = new HashMap<>();
        PriorityQueue<PathCandidate<N>> queue = new PriorityQueue<>(
                Comparator.<PathCandidate<N>, BigDecimal>comparing(PathCandidate::distance)
                        .thenComparingLong(PathCandidate::sequence));
        long sequence = 0;
        distances.put(from, BigDecimal.ZERO);
        queue.add(new PathCandidate<>(from, BigDecimal.ZERO, sequence++));

        while (!queue.isEmpty()) {
            PathCandidate<N> current = queue.remove();
            if (current.distance().compareTo(distances.get(current.node())) > 0) {
                continue;
            }
            if (Objects.equals(current.node(), to)) {
                break;
            }

            for (Map.Entry<N, StochasticTime> edge : adjacency.get(current.node()).entrySet()) {
                N neighbor = edge.getKey();
                BigDecimal nextDistance = current.distance().add(edge.getValue().getLFT());
                BigDecimal oldDistance = distances.get(neighbor);
                if (oldDistance == null || nextDistance.compareTo(oldDistance) < 0) {
                    distances.put(neighbor, nextDistance);
                    previous.put(neighbor, current.node());
                    queue.add(new PathCandidate<>(neighbor, nextDistance, sequence++));
                }
            }
        }

        if (!distances.containsKey(to)) {
            return Optional.empty();
        }

        LinkedList<N> path = new LinkedList<>();
        for (N node = to; node != null; node = previous.get(node)) {
            path.addFirst(node);
            if (Objects.equals(node, from)) {
                return Optional.of(List.copyOf(path));
            }
        }
        return Optional.empty();
    }

    private void requireNode(N id) {
        if (!adjacency.containsKey(id)) {
            throw new NoSuchElementException("Node not found: " + id);
        }
    }

    private record PathCandidate<N>(N node, BigDecimal distance, long sequence) { }
}