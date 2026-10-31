package org.oristool.rospino2.placement;

import org.junit.jupiter.api.Test;
import org.oristool.eulero.modeling.Activity;
import org.oristool.eulero.modeling.Composite;
import org.oristool.eulero.modeling.Simple;
import org.oristool.eulero.modeling.stochastictime.UniformTime;
import org.oristool.rospino2.computingNodes.AbstractNode;
import org.oristool.rospino2.computingNodes.BidirectionalWeightedGraph;
import org.oristool.rospino2.computingNodes.NetworkGraph;
import org.oristool.rospino2.computingNodes.RospinoNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimplePlacementBuilderTest {
    private final SimplePlacementBuilder builder = new SimplePlacementBuilder();

    @Test
    void topologicalSortReturnsOnlySimpleActivitiesInDependencyOrder() {
        Simple a = simple("A");
        Simple b = simple("B");
        Simple c = simple("C");
        Simple d = simple("D");
        b.addPrecondition(a);
        c.addPrecondition(a);
        d.addPrecondition(b, c);
        Service service = new Service(composite(a, b, c, d));

        List<Activity> result = builder.topologicalSort(service);

        assertEquals(4, result.size());
        assertTrue(result.stream().allMatch(Simple.class::isInstance));
        assertBefore(result, a, b);
        assertBefore(result, a, c);
        assertBefore(result, b, d);
        assertBefore(result, c, d);
    }

    @Test
    void topologicalSortPlacesEverySimpleInCompositePredecessorBeforeDependent() {
        Simple first = simple("first");
        Simple nested = simple("nested");
        Simple last = simple("last");
        Composite inner = composite(first, nested);
        Composite predecessor = composite(inner, last);
        Simple dependent = simple("dependent");
        dependent.addPrecondition(predecessor);

        // The dependent is traversed before its prerequisite to make traversal order
        // insufficient on its own to satisfy the expected result.
        Service service = new Service(composite(dependent, predecessor));

        List<Activity> result = builder.topologicalSort(service);

        assertEquals(List.of(first, nested, last, dependent), result);
        assertTrue(result.stream().allMatch(Simple.class::isInstance));
    }

    @Test
    void topologicalSortRejectsCycles() {
        Simple a = simple("A");
        Simple b = simple("B");
        a.addPrecondition(b);
        b.addPrecondition(a);

        assertThrows(IllegalArgumentException.class,
                () -> builder.topologicalSort(new Service(composite(a, b))));
    }

    @Test
    void buildChoosesNodeWithLowestWeightedShortestPath() {
        Simple predecessor = simple("predecessor");
        Simple dependent = simple("dependent");
        dependent.addPrecondition(predecessor);
        Service service = new Service(composite(predecessor, dependent));

        RospinoNode source = node(1, 1);
        RospinoNode directCandidate = node(2, 1);
        RospinoNode intermediate = node(3, 0);
        RospinoNode shortestPathCandidate = node(4, 1);
        BidirectionalWeightedGraph<AbstractNode> graph = graphWith(
                source, directCandidate, intermediate, shortestPathCandidate);
        graph.addEdge(source, directCandidate, new UniformTime(7, 7));
        graph.addEdge(source, intermediate, new UniformTime(2, 2));
        graph.addEdge(intermediate, shortestPathCandidate, new UniformTime(2, 2));
        graph.addEdge(source, shortestPathCandidate, new UniformTime(8, 8));

        Placement placement = build(service, graph);

        assertSame(source, placement.getAssignments().get(predecessor));
        assertSame(shortestPathCandidate, placement.getAssignments().get(dependent));
        assertEquals(0.0, source.getAvailableCores(), 0.00001);
        assertEquals(0.0, shortestPathCandidate.getAvailableCores(), 0.00001);
    }

    @Test
    void buildSkipsCheapestNodeWhenItCannotSatisfyResourceDemand() {
        Simple predecessor = simple("predecessor");
        Simple dependent = simple("dependent");
        dependent.addPrecondition(predecessor);
        Service service = new Service(composite(predecessor, dependent));
        service.setResourcesRequired(dependent, 2);

        RospinoNode source = node(1, 1);
        RospinoNode cheaperButTooSmall = node(2, 1);
        RospinoNode feasible = node(3, 2);
        BidirectionalWeightedGraph<AbstractNode> graph = graphWith(source, cheaperButTooSmall, feasible);
        graph.addEdge(source, cheaperButTooSmall, new UniformTime(1, 1));
        graph.addEdge(source, feasible, new UniformTime(4, 4));

        Placement placement = build(service, graph);

        assertSame(source, placement.getAssignments().get(predecessor));
        assertSame(feasible, placement.getAssignments().get(dependent));
        assertEquals(1.0, cheaperButTooSmall.getAvailableCores(), 0.00001);
        assertEquals(0.0, feasible.getAvailableCores(), 0.00001);
    }

    @Test
    void buildLeavesNodeResourcesUnchangedWhenPlacementIsImpossible() {
        Simple predecessor = simple("predecessor");
        Simple dependent = simple("dependent");
        dependent.addPrecondition(predecessor);
        Service service = new Service(composite(predecessor, dependent));
        service.setResourcesRequired(dependent, 2);

        RospinoNode onlyNode = node(1, 2);
        BidirectionalWeightedGraph<AbstractNode> graph = graphWith(onlyNode);

        assertThrows(IllegalStateException.class, () -> build(service, graph));
        assertEquals(2.0, onlyNode.getAvailableCores(), 0.00001);
    }

    private static void assertBefore(List<Activity> ordered, Activity first, Activity second) {
        assertTrue(ordered.indexOf(first) < ordered.indexOf(second),
                () -> first.name() + " must precede " + second.name());
    }

    private static Composite composite(Activity... children) {
        Composite composite = new Composite();
        composite.setActivities(List.of(children));
        return composite;
    }

    private static Simple simple(String name) {
        return new Simple(name, new UniformTime(1, 1));
    }

    private static RospinoNode node(int id, double cores) {
        return new RospinoNode(id, cores);
    }

    private static BidirectionalWeightedGraph<AbstractNode> graphWith(AbstractNode... nodes) {
        BidirectionalWeightedGraph<AbstractNode> graph = new BidirectionalWeightedGraph<>();
        for (AbstractNode node : nodes) {
            graph.addNode(node);
        }
        return graph;
    }

    private static Placement build(Service service, BidirectionalWeightedGraph<AbstractNode> graph) {
        SimplePlacementBuilder builder = new SimplePlacementBuilder();
        builder.setWorkflow(service);
        builder.setNetworkGraph(new NetworkGraph(graph));
        return builder.build();
    }
}
