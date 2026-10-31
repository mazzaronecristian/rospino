package org.oristool.rospino2.computingNodes;

import org.junit.jupiter.api.Test;
import org.oristool.eulero.modeling.stochastictime.StochasticTime;
import org.oristool.eulero.modeling.stochastictime.UniformTime;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BidirectionalWeightedGraphTest {
    @Test
    void storesWeightedEdgesInBothDirectionsAndAllowsIsolatedNodes() {
        BidirectionalWeightedGraph<String> graph = new BidirectionalWeightedGraph<>();
        StochasticTime weight = new UniformTime(2, 5);
        graph.addNode("E");
        graph.addEdge("D", "E", weight);

        assertEquals(2, graph.nodeCount());
        assertEquals(1, graph.edgeCount());
        assertEquals(weight, graph.getWeight("D", "E").orElseThrow());
        assertEquals(weight, graph.getWeight("E", "D").orElseThrow());
        assertTrue(graph.hasNode("E"));
        assertFalse(graph.hasEdge("D", "D"));
    }

    @Test
    void returnsTheLowestTotalLftPathIncludingEndpoints() {
        BidirectionalWeightedGraph<String> graph = new BidirectionalWeightedGraph<>();
        graph.addEdge("A", "B", new UniformTime(5, 5));
        graph.addEdge("A", "C", new UniformTime(1, 1));
        graph.addEdge("C", "B", new UniformTime(1, 1));

        assertEquals(List.of("A", "C", "B"), graph.shortestPath("A", "B").orElseThrow());
        assertEquals(List.of("B", "C", "A"), graph.shortestPath("B", "A").orElseThrow());
    }

    @Test
    void comparesTheSumOfAllEdgesRatherThanChoosingTheLightestFirstEdge() {
        BidirectionalWeightedGraph<String> graph = new BidirectionalWeightedGraph<>();
        graph.addEdge("A", "B", new UniformTime(2, 2));
        graph.addEdge("B", "D", new UniformTime(20, 20));
        graph.addEdge("A", "C", new UniformTime(4, 4));
        graph.addEdge("C", "E", new UniformTime(4, 4));
        graph.addEdge("E", "D", new UniformTime(4, 4));

        assertEquals(List.of("A", "C", "E", "D"), graph.shortestPath("A", "D").orElseThrow());
    }

    @Test
    void usesLftInsteadOfEftToCompareEdgeWeights() {
        BidirectionalWeightedGraph<String> graph = new BidirectionalWeightedGraph<>();
        graph.addEdge("A", "B", new UniformTime(1, 10));
        graph.addEdge("A", "C", new UniformTime(2, 2));
        graph.addEdge("C", "B", new UniformTime(2, 2));

        assertEquals(List.of("A", "C", "B"), graph.shortestPath("A", "B").orElseThrow());
    }

    @Test
    void keepsTheFirstDiscoveredPathWhenTotalCostsTie() {
        BidirectionalWeightedGraph<String> graph = new BidirectionalWeightedGraph<>();
        graph.addEdge("S", "A", new UniformTime(1, 1));
        graph.addEdge("S", "B", new UniformTime(1, 1));
        graph.addEdge("A", "T", new UniformTime(1, 1));
        graph.addEdge("B", "T", new UniformTime(1, 1));

        assertEquals(List.of("S", "A", "T"), graph.shortestPath("S", "T").orElseThrow());
    }

    @Test
    void handlesSameNodeDisconnectedNodesAndUnknownNodes() {
        BidirectionalWeightedGraph<String> graph = new BidirectionalWeightedGraph<>();
        graph.addNode("A");
        graph.addNode("B");

        assertEquals(List.of("A"), graph.shortestPath("A", "A").orElseThrow());
        assertTrue(graph.shortestPath("A", "B").isEmpty());
        assertThrows(NoSuchElementException.class, () -> graph.shortestPath("A", "missing"));
    }

    @Test
    void rejectsNullAndNegativeLftWeights() {
        BidirectionalWeightedGraph<String> graph = new BidirectionalWeightedGraph<>();
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge("A", "B", null));
        assertThrows(IllegalArgumentException.class,
                () -> graph.addEdge("A", "B", new UniformTime(-1, -1)));
    }
}
