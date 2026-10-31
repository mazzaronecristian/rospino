package org.oristool.rospino2.computingNodes;

import org.junit.jupiter.api.Test;
import org.oristool.eulero.modeling.stochastictime.UniformTime;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListBidirectionalWeightedGraphTest {
    @Test
    void addsNodesAndWeightedEdgesInBothDirectionsAndUpdatesExistingEdges() {
        ListBidirectionalWeightedGraph<String> graph = new ListBidirectionalWeightedGraph<>();
        graph.addNode("isolated");
        graph.addEdge("A", "B", new UniformTime(1, 2));

        assertEquals(3, graph.nodeCount());
        assertEquals(1, graph.edgeCount());
        assertTrue(graph.hasEdge("A", "B"));
        assertTrue(graph.hasEdge("B", "A"));
        assertEquals(new UniformTime(1, 2).getLFT(), graph.getWeight("B", "A").orElseThrow().getLFT());
        assertFalse(graph.hasEdge("isolated", "A"));

        graph.addEdge("A", "B", new UniformTime(4, 5));
        assertEquals(1, graph.edgeCount());
        assertEquals(new UniformTime(4, 5).getLFT(), graph.getWeight("B", "A").orElseThrow().getLFT());
        assertEquals(List.of("isolated", "A", "B"), List.copyOf(graph.nodes()));
    }

    @Test
    void findsMinimumTotalLftPathAndReturnsItInEitherDirection() {
        ListBidirectionalWeightedGraph<String> graph = new ListBidirectionalWeightedGraph<>();
        graph.addEdge("A", "B", new UniformTime(1, 10));
        graph.addEdge("A", "C", new UniformTime(2, 2));
        graph.addEdge("C", "D", new UniformTime(2, 2));
        graph.addEdge("D", "B", new UniformTime(2, 2));

        assertEquals(List.of("A", "C", "D", "B"), graph.shortestPath("A", "B").orElseThrow());
        assertEquals(List.of("B", "D", "C", "A"), graph.shortestPath("B", "A").orElseThrow());
    }

    @Test
    void handlesTiesSameNodeDisconnectedNodesAndUnknownNodes() {
        ListBidirectionalWeightedGraph<String> graph = new ListBidirectionalWeightedGraph<>();
        graph.addEdge("S", "A", new UniformTime(1, 1));
        graph.addEdge("S", "B", new UniformTime(1, 1));
        graph.addEdge("A", "T", new UniformTime(1, 1));
        graph.addEdge("B", "T", new UniformTime(1, 1));
        graph.addNode("isolated");

        assertEquals(List.of("S", "A", "T"), graph.shortestPath("S", "T").orElseThrow());
        assertEquals(List.of("S"), graph.shortestPath("S", "S").orElseThrow());
        assertTrue(graph.shortestPath("S", "isolated").isEmpty());
        assertThrows(NoSuchElementException.class, () -> graph.shortestPath("missing", "S"));
    }

    @Test
    void rejectsNullAndNegativeWeight() {
        ListBidirectionalWeightedGraph<String> graph = new ListBidirectionalWeightedGraph<>();

        assertThrows(IllegalArgumentException.class, () -> graph.addEdge("A", "B", null));
        assertThrows(IllegalArgumentException.class,
                () -> graph.addEdge("A", "B", new UniformTime(-1, -1)));
    }
}
