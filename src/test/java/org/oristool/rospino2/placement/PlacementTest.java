package org.oristool.rospino2.placement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.oristool.eulero.modeling.Activity;
import org.oristool.eulero.modeling.Simple;
import org.oristool.eulero.modeling.stochastictime.StochasticTime;
import org.oristool.eulero.modeling.stochastictime.UniformTime;
import org.oristool.rospino2.computingNodes.AbstractNode;
import org.oristool.rospino2.computingNodes.NetworkGraph;
import org.oristool.rospino2.computingNodes.RospinoNode;
import org.oristool.rospino2.computingNodes.StronglyConnectedWeightedGraph;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Placement.buildWorkflow")
class PlacementTest {

    @Test
    @DisplayName("inserisce un Simple con il delay di rete tra due attività in sequenza")
    void insertsNetworkDelayBetweenTwoActivities() {
        AbstractNode node1 = node(1);
        AbstractNode node2 = node(2);
        UniformTime networkDelay = new UniformTime(2, 5);
        StronglyConnectedWeightedGraph<AbstractNode, StochasticTime> graph = graphWith(node1, node2);
        graph.addEdge(node1, node2, networkDelay);

        Simple activityA = task("A");
        Simple activityB = task("B");
        activityB.addPrecondition(activityA);

        Activity result = placement(graph, activityA, node1, activityB, node2).buildWorkflow();

        assertSame(activityA, result);
        assertTrue(activityA.pre().isEmpty());
        Simple networkActivity = assertTransition(activityA, activityB, "B_A", networkDelay);
        assertEquals(List.of(networkActivity), activityB.pre());
    }

    @Test
    @DisplayName("mantiene una Simple di rete per ogni arco in una sequenza di tre attività")
    void insertsNetworkDelaysThroughoutASequence() {
        AbstractNode node1 = node(1);
        AbstractNode node2 = node(2);
        AbstractNode node3 = node(3);
        UniformTime delay12 = new UniformTime(1, 2);
        UniformTime delay23 = new UniformTime(3, 4);
        StronglyConnectedWeightedGraph<AbstractNode, StochasticTime> graph = graphWith(node1, node2, node3);
        graph.addEdge(node1, node2, delay12);
        graph.addEdge(node2, node3, delay23);

        Simple activityA = task("A");
        Simple activityB = task("B");
        Simple activityC = task("C");
        activityB.addPrecondition(activityA);
        activityC.addPrecondition(activityB);

        Activity result = placement(graph,
                activityA, node1,
                activityB, node2,
                activityC, node3).buildWorkflow();

        assertSame(activityA, result);
        assertTransition(activityA, activityB, "B_A", delay12);
        assertTransition(activityB, activityC, "C_B", delay23);
        assertEquals(Set.of("B_A"), names(activityA.post()));
        assertEquals(Set.of("C_B"), names(activityB.post()));
    }

    @Test
    @DisplayName("gestisce un fork e un join con delay distinti per ogni dipendenza")
    void insertsNetworkDelaysForForkAndJoin() {
        AbstractNode node1 = node(1);
        AbstractNode node2 = node(2);
        AbstractNode node3 = node(3);
        AbstractNode node4 = node(4);
        UniformTime delayAB = new UniformTime(1, 2);
        UniformTime delayAC = new UniformTime(2, 3);
        UniformTime delayBD = new UniformTime(3, 4);
        UniformTime delayCD = new UniformTime(4, 5);
        StronglyConnectedWeightedGraph<AbstractNode, StochasticTime> graph = graphWith(node1, node2, node3, node4);
        graph.addEdge(node1, node2, delayAB);
        graph.addEdge(node1, node3, delayAC);
        graph.addEdge(node2, node4, delayBD);
        graph.addEdge(node3, node4, delayCD);

        Simple activityA = task("A");
        Simple activityB = task("B");
        Simple activityC = task("C");
        Simple activityD = task("D");
        activityB.addPrecondition(activityA);
        activityC.addPrecondition(activityA);
        activityD.addPrecondition(activityB, activityC);

        Activity result = placement(graph,
                activityA, node1,
                activityB, node2,
                activityC, node3,
                activityD, node4).buildWorkflow();

        assertSame(activityA, result);
        assertTransition(activityA, activityB, "B_A", delayAB);
        assertTransition(activityA, activityC, "C_A", delayAC);
        assertTransition(activityB, activityD, "D_B", delayBD);
        assertTransition(activityC, activityD, "D_C", delayCD);

        assertEquals(Set.of("B_A", "C_A"), names(activityA.post()));
        assertEquals(Set.of("D_B", "D_C"), names(activityD.pre()));
        assertTrue(activityD.post().isEmpty());
    }

    private static Simple assertTransition(Activity from, Activity to, String name, StochasticTime delay) {
        Activity transitionActivity = from.post().stream()
                .filter(Simple.class::isInstance)
                .filter(activity -> name.equals(activity.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Manca la Simple " + name + " tra le postcondizioni di " + from.name()));
        Simple transition = assertInstanceOf(Simple.class, transitionActivity);

        assertInstanceOf(UniformTime.class, transition.getPdf());
        assertEquals(delay.getEFT(), transition.getPdf().getEFT());
        assertEquals(delay.getLFT(), transition.getPdf().getLFT());
        assertEquals(List.of(from), transition.pre());
        assertEquals(List.of(to), transition.post());
        assertTrue(to.pre().contains(transition));
        assertTrue(from.post().contains(transition));
        assertTrue(from.post().stream().allMatch(Simple.class::isInstance));
        return transition;
    }

    private static Set<String> names(List<Activity> activities) {
        return activities.stream().map(Activity::name).collect(Collectors.toSet());
    }

    private static Placement placement(StronglyConnectedWeightedGraph<AbstractNode, StochasticTime> graph,
                                       Activity activityA, AbstractNode nodeA,
                                       Activity activityB, AbstractNode nodeB) {
        Map<Activity, AbstractNode> assignments = new LinkedHashMap<>();
        assignments.put(activityA, nodeA);
        assignments.put(activityB, nodeB);
        return new Placement(assignments, new NetworkGraph(graph));
    }

    private static Placement placement(StronglyConnectedWeightedGraph<AbstractNode, StochasticTime> graph,
                                       Activity activityA, AbstractNode nodeA,
                                       Activity activityB, AbstractNode nodeB,
                                       Activity activityC, AbstractNode nodeC) {
        Map<Activity, AbstractNode> assignments = new LinkedHashMap<>();
        assignments.put(activityA, nodeA);
        assignments.put(activityB, nodeB);
        assignments.put(activityC, nodeC);
        return new Placement(assignments, new NetworkGraph(graph));
    }

    private static Placement placement(StronglyConnectedWeightedGraph<AbstractNode, StochasticTime> graph,
                                       Activity activityA, AbstractNode nodeA,
                                       Activity activityB, AbstractNode nodeB,
                                       Activity activityC, AbstractNode nodeC,
                                       Activity activityD, AbstractNode nodeD) {
        Map<Activity, AbstractNode> assignments = new LinkedHashMap<>();
        assignments.put(activityA, nodeA);
        assignments.put(activityB, nodeB);
        assignments.put(activityC, nodeC);
        assignments.put(activityD, nodeD);
        return new Placement(assignments, new NetworkGraph(graph));
    }

    private static StronglyConnectedWeightedGraph<AbstractNode, StochasticTime> graphWith(AbstractNode... nodes) {
        StronglyConnectedWeightedGraph<AbstractNode, StochasticTime> graph = new StronglyConnectedWeightedGraph<>();
        for (AbstractNode node : nodes) {
            graph.addNode(node, new UniformTime(0, 0));
        }
        return graph;
    }

    private static AbstractNode node(int id) {
        return new RospinoNode(id, 4);
    }

    private static Simple task(String name) {
        return new Simple(name, new UniformTime(1, 1));
    }
}