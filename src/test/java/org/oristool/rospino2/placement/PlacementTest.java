package org.oristool.rospino2.placement;

import org.junit.jupiter.api.Test;
import org.oristool.eulero.modeling.Activity;
import org.oristool.eulero.modeling.Composite;
import org.oristool.eulero.modeling.Simple;
import org.oristool.eulero.modeling.stochastictime.StochasticTime;
import org.oristool.eulero.modeling.stochastictime.UniformTime;
import org.oristool.rospino2.computingNodes.AbstractNode;
import org.oristool.rospino2.computingNodes.BidirectionalWeightedGraph;
import org.oristool.rospino2.computingNodes.NetworkGraph;
import org.oristool.rospino2.computingNodes.RospinoNode;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacementTest {
    @Test
    void buildsThePicturedWorkflowWithoutTransfersWhenAllActivitiesShareOneNode() {
        PhotoWorkflow photo = photoWorkflow();
        RospinoNode onlyNode = node(1);
        BidirectionalWeightedGraph<AbstractNode> graph = graphWith(onlyNode);
        Map<Activity, AbstractNode> assignments = assignments(
                photo, onlyNode, onlyNode, onlyNode, onlyNode, onlyNode, onlyNode);

        Activity result = placement(new Service(photo.root()), graph, assignments).buildWorkflow();

        assertSame(photo.root(), result);
        assertEquals(6, photo.root().activities().size());
        assertTrue(generatedHops(photo.root()).isEmpty());
        assertEquals(List.of(photo.start()), photo.composites().get("Q").pre());
        assertEquals(List.of(photo.start()), photo.composites().get("R").pre());
        assertEquals(List.of(photo.composites().get("Q"), photo.composites().get("R")),
                photo.composites().get("S").pre());
        assertEquals(List.of(photo.composites().get("R")), photo.composites().get("T").pre());
        assertEquals(List.of(photo.composites().get("S"), photo.composites().get("T")), photo.end().pre());
    }

    @Test
    void buildsThePicturedWorkflowWithOneTransferPerCrossNodeDependencyOnACompleteGraph() {
        PhotoWorkflow photo = photoWorkflow();
        RospinoNode qAndRNode = node(1);
        RospinoNode sNode = node(2);
        RospinoNode tNode = node(3);
        RospinoNode endNode = node(4);
        BidirectionalWeightedGraph<AbstractNode> graph = graphWith(qAndRNode, sNode, tNode, endNode);
        UniformTime directDelay = new UniformTime(1, 1);
        graph.addEdge(qAndRNode, sNode, directDelay);
        graph.addEdge(qAndRNode, tNode, directDelay);
        graph.addEdge(qAndRNode, endNode, directDelay);
        graph.addEdge(sNode, tNode, directDelay);
        graph.addEdge(sNode, endNode, directDelay);
        graph.addEdge(tNode, endNode, directDelay);
        Map<Activity, AbstractNode> assignments = assignments(
                photo, qAndRNode, qAndRNode, qAndRNode, sNode, tNode, endNode);

        Activity result = placement(new Service(photo.root()), graph, assignments).buildWorkflow();

        assertSame(photo.root(), result);
        assertEquals(84, generatedHops(photo.root()).size());
        assertEquals(90, photo.root().activities().size());
        for (Simple hop : generatedHops(photo.root())) {
            assertEquals(1, hop.pre().size());
            assertEquals(1, hop.post().size());
            Simple source = assertInstanceOf(Simple.class, hop.pre().getFirst());
            Simple target = assertInstanceOf(Simple.class, hop.post().getFirst());
            assertTrue(assignments.get(source) != assignments.get(target));
            assertSame(directDelay, hop.getPdf());
        }
    }

    @Test
    void buildsThePicturedWorkflowAcrossAThreeNodeLine() {
        PhotoWorkflow photo = photoWorkflow();
        RospinoNode first = node(1);
        RospinoNode middle = node(2);
        RospinoNode last = node(3);
        BidirectionalWeightedGraph<AbstractNode> graph = graphWith(first, middle, last);
        UniformTime firstDelay = new UniformTime(1, 2);
        UniformTime secondDelay = new UniformTime(3, 5);
        graph.addEdge(first, middle, firstDelay);
        graph.addEdge(middle, last, secondDelay);
        Map<Activity, AbstractNode> assignments = assignments(
                photo, first, first, middle, last, middle, last);

        Activity result = placement(new Service(photo.root()), graph, assignments).buildWorkflow();

        assertSame(photo.root(), result);
        assertEquals(82, generatedHops(photo.root()).size());
        assertEquals(88, photo.root().activities().size());
        assertTransferChain(photo, "Q1A", "S1A", firstDelay, secondDelay);
        assertSingleTransfer(photo, "R1A", "S1A", secondDelay);
        assertFalse(hasHop(photo, "R1A", "T1A"));
        assertSingleTransfer(photo, "T1A", "end", secondDelay);
    }

    @Test
    void buildsThePicturedWorkflowUsingTheCheaperTwoEdgeRoute() {
        PhotoWorkflow photo = photoWorkflow();
        RospinoNode first = node(1);
        RospinoNode middle = node(2);
        RospinoNode last = node(3);
        BidirectionalWeightedGraph<AbstractNode> graph = graphWith(first, middle, last);
        UniformTime firstDelay = new UniformTime(1, 2);
        UniformTime secondDelay = new UniformTime(2, 3);
        UniformTime expensiveDirectDelay = new UniformTime(1, 9);
        graph.addEdge(first, middle, firstDelay);
        graph.addEdge(middle, last, secondDelay);
        graph.addEdge(first, last, expensiveDirectDelay);
        Map<Activity, AbstractNode> assignments = assignments(
                photo, first, first, first, last, middle, last);

        Activity result = placement(new Service(photo.root()), graph, assignments).buildWorkflow();

        assertSame(photo.root(), result);
        assertEquals(126, generatedHops(photo.root()).size());
        assertTransferChain(photo, "Q1A", "S1A", firstDelay, secondDelay);
        assertFalse(hasHopWithPdf(photo, expensiveDirectDelay));
    }

    @Test
    void rejectsDisconnectedNetworkWithoutChangingThePicturedWorkflow() {
        PhotoWorkflow photo = photoWorkflow();
        RospinoNode first = node(1);
        RospinoNode disconnected = node(3);
        BidirectionalWeightedGraph<AbstractNode> graph = graphWith(first, disconnected);
        Map<Activity, AbstractNode> assignments = assignments(
                photo, first, first, first, disconnected, disconnected, disconnected);
        Placement placement = placement(new Service(photo.root()), graph, assignments);
        ActivitySnapshot before = snapshot(photo.root());

        assertThrows(IllegalStateException.class, placement::buildWorkflow);

        assertUnchanged(before);
        assertTrue(generatedHops(photo.root()).isEmpty());
    }

    @Test
    void insertsOneSimpleForADirectNetworkEdgeAndReturnsTheServiceRoot() {
        Simple a = simple("A");
        Simple b = simple("B");
        b.addPrecondition(a);
        Composite root = composite(a, b);
        Service service = new Service(root);
        RospinoNode first = node(1);
        RospinoNode second = node(2);
        UniformTime delay = new UniformTime(2, 5);
        BidirectionalWeightedGraph<AbstractNode> graph = graphWith(first, second);
        graph.addEdge(first, second, delay);
        Placement placement = placement(service, graph, Map.of(a, first, b, second));

        assertSame(root, placement.buildWorkflow());

        Simple hop = onlySuccessor(a, delay);
        assertEquals("A_B_hop1", hop.name());
        assertEquals(List.of(hop), b.pre());
        assertEquals(List.of(b), hop.post());
        assertEquals(List.of(a, b, hop), root.activities());
        assertFalse(b.pre().contains(a));
        assertEquals(Set.of(a, b), service.getResourcesPerSimple().keySet());

        assertSame(root, placement.buildWorkflow());
        assertEquals(3, root.activities().size());
        assertEquals(List.of(hop), a.post());
    }

    @Test
    void insertsOneSimplePerEdgeOfTheLowestLftPath() {
        Simple a = simple("A");
        Simple b = simple("B");
        b.addPrecondition(a);
        Composite root = composite(a, b);
        RospinoNode first = node(1);
        RospinoNode middle = node(2);
        RospinoNode last = node(3);
        UniformTime firstDelay = new UniformTime(1, 2);
        UniformTime secondDelay = new UniformTime(1, 3);
        UniformTime expensiveDirectDelay = new UniformTime(1, 10);
        BidirectionalWeightedGraph<AbstractNode> graph = graphWith(first, middle, last);
        graph.addEdge(first, middle, firstDelay);
        graph.addEdge(middle, last, secondDelay);
        graph.addEdge(first, last, expensiveDirectDelay);

        placement(new Service(root), graph, Map.of(a, first, b, last)).buildWorkflow();

        Simple hop1 = onlySuccessor(a, firstDelay);
        Simple hop2 = onlySuccessor(hop1, secondDelay);
        assertEquals(List.of(hop2), b.pre());
        assertEquals(List.of(b), hop2.post());
        assertEquals(List.of(a, b, hop1, hop2), root.activities());
    }

    @Test
    void insertsSeparateChainsForForkAndJoinDependencies() {
        Simple a = simple("A");
        Simple b = simple("B");
        Simple c = simple("C");
        Simple d = simple("D");
        b.addPrecondition(a);
        c.addPrecondition(a);
        d.addPrecondition(b, c);
        Composite root = composite(a, b, c, d);
        RospinoNode first = node(1);
        RospinoNode second = node(2);
        RospinoNode third = node(3);
        RospinoNode fourth = node(4);
        BidirectionalWeightedGraph<AbstractNode> graph = graphWith(first, second, third, fourth);
        graph.addEdge(first, second, new UniformTime(1, 2));
        graph.addEdge(first, third, new UniformTime(1, 3));
        graph.addEdge(second, fourth, new UniformTime(1, 4));
        graph.addEdge(third, fourth, new UniformTime(1, 5));

        placement(new Service(root), graph,
                Map.of(a, first, b, second, c, third, d, fourth)).buildWorkflow();

        assertEquals(8, root.activities().size());
        assertEquals(2, a.post().size());
        assertEquals(1, b.pre().size());
        assertEquals(1, c.pre().size());
        assertEquals(2, d.pre().size());
        assertEquals(Set.of(a), b.pre().stream().map(hop -> hop.pre().getFirst()).collect(Collectors.toSet()));
        assertEquals(Set.of(a), c.pre().stream().map(hop -> hop.pre().getFirst()).collect(Collectors.toSet()));
        assertEquals(Set.of(b, c), d.pre().stream().map(hop -> hop.pre().getFirst()).collect(Collectors.toSet()));
        assertTrue(root.activities().subList(4, 8).stream().allMatch(Simple.class::isInstance));
    }

    @Test
    void expandsCompositePreconditionsWithoutRemovingTheCompositeLink() {
        Simple a = simple("A");
        Simple b = simple("B");
        Simple c = simple("C");
        Simple d = simple("D");
        Composite predecessor = composite(a, b);
        Composite dependent = composite(c, d);
        dependent.addPrecondition(predecessor);
        Composite root = composite(predecessor, dependent);
        Service service = new Service(root);
        RospinoNode first = node(1);
        RospinoNode second = node(2);
        UniformTime delay = new UniformTime(1, 2);
        BidirectionalWeightedGraph<AbstractNode> graph = graphWith(first, second);
        graph.addEdge(first, second, delay);

        placement(service, graph, Map.of(a, first, b, first, c, second, d, second)).buildWorkflow();

        assertEquals(List.of(predecessor), dependent.pre());
        assertEquals(6, root.activities().size());
        assertEquals(Set.of(a, b), c.pre().stream().map(hop -> hop.pre().getFirst()).collect(Collectors.toSet()));
        assertEquals(Set.of(a, b), d.pre().stream().map(hop -> hop.pre().getFirst()).collect(Collectors.toSet()));
        assertTrue(root.activities().subList(2, 6).stream()
                .map(Simple.class::cast).allMatch(hop -> hop.getPdf() == delay));
        assertEquals(Set.of(a, b, c, d), service.getResourcesPerSimple().keySet());
    }

    @Test
    void expandsDependenciesAcrossNestedCompositesUsingEveryPathEdge() {
        Simple a = simple("A");
        Simple b = simple("B");
        Simple c = simple("C");
        Simple d = simple("D");
        Composite sourceInner = composite(a, b);
        Composite predecessor = composite(sourceInner);
        Composite targetInner = composite(c, d);
        Composite dependent = composite(targetInner);
        dependent.addPrecondition(predecessor);
        Composite group = composite(predecessor, dependent);
        Composite root = composite(group);
        Service service = new Service(root);

        RospinoNode first = node(1);
        RospinoNode middle = node(2);
        RospinoNode last = node(3);
        UniformTime firstDelay = new UniformTime(1, 2);
        UniformTime secondDelay = new UniformTime(3, 5);
        BidirectionalWeightedGraph<AbstractNode> graph = graphWith(first, middle, last);
        graph.addEdge(first, middle, firstDelay);
        graph.addEdge(middle, last, secondDelay);

        Placement placement = placement(service, graph,
                Map.of(a, first, b, first, c, last, d, last));

        assertSame(root, placement.buildWorkflow());

        assertSame(group, root.activities().getFirst());
        assertSame(sourceInner, predecessor.activities().getFirst());
        assertSame(targetInner, dependent.activities().getFirst());
        assertEquals(List.of(predecessor), dependent.pre());
        assertEquals(9, root.activities().size());
        for (Simple source : List.of(a, b)) {
            for (Simple target : List.of(c, d)) {
                Simple hop2 = target.pre().stream()
                        .map(activity -> assertInstanceOf(Simple.class, activity))
                        .filter(hop -> hop.name().equals(source.name() + "_" + target.name() + "_hop2"))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("Manca il secondo hop per "
                                + source.name() + " -> " + target.name()));
                Simple hop1 = assertInstanceOf(Simple.class, hop2.pre().getFirst());

                assertEquals(source.name() + "_" + target.name() + "_hop1", hop1.name());
                assertSame(firstDelay, hop1.getPdf());
                assertSame(secondDelay, hop2.getPdf());
                assertEquals(List.of(source), hop1.pre());
                assertEquals(List.of(hop1), hop2.pre());
                assertEquals(List.of(hop2), hop1.post());
                assertEquals(List.of(target), hop2.post());
            }
        }
        assertEquals(Set.of(a, b, c, d), service.getResourcesPerSimple().keySet());
    }

    @Test
    void leavesDependenciesOnTheSameNodeUnchanged() {
        Simple a = simple("A");
        Simple b = simple("B");
        b.addPrecondition(a);
        Composite root = composite(a, b);
        RospinoNode node = node(1);
        BidirectionalWeightedGraph<AbstractNode> graph = graphWith(node);

        assertSame(root, placement(new Service(root), graph, Map.of(a, node, b, node)).buildWorkflow());

        assertEquals(List.of(a, b), root.activities());
        assertEquals(List.of(a), b.pre());
        assertEquals(List.of(b), a.post());
    }

    @Test
    void rejectsAnUnreachableNodeBeforeChangingTheWorkflow() {
        Simple a = simple("A");
        Simple b = simple("B");
        b.addPrecondition(a);
        Composite root = composite(a, b);
        RospinoNode first = node(1);
        RospinoNode second = node(2);
        BidirectionalWeightedGraph<AbstractNode> graph = graphWith(first, second);
        Placement placement = placement(new Service(root), graph, Map.of(a, first, b, second));

        assertThrows(IllegalStateException.class, placement::buildWorkflow);

        assertEquals(List.of(a, b), root.activities());
        assertEquals(List.of(a), b.pre());
        assertEquals(List.of(b), a.post());
    }

    @Test
    void rejectsMissingAssignmentsBeforeChangingTheWorkflow() {
        Simple a = simple("A");
        Simple b = simple("B");
        b.addPrecondition(a);
        Composite root = composite(a, b);
        RospinoNode first = node(1);
        RospinoNode second = node(2);
        BidirectionalWeightedGraph<AbstractNode> graph = graphWith(first, second);
        graph.addEdge(first, second, new UniformTime(1, 2));
        Placement placement = placement(new Service(root), graph, Map.of(a, first));

        assertThrows(IllegalStateException.class, placement::buildWorkflow);

        assertEquals(List.of(a, b), root.activities());
        assertEquals(List.of(a), b.pre());
        assertEquals(List.of(b), a.post());
    }

    private static Simple onlySuccessor(Activity from, StochasticTime expectedDelay) {
        assertEquals(1, from.post().size());
        Simple hop = assertInstanceOf(Simple.class, from.post().getFirst());
        assertSame(expectedDelay, hop.getPdf());
        assertEquals(List.of(from), hop.pre());
        return hop;
    }

    private static PhotoWorkflow photoWorkflow() {
        Map<String, Simple> leaves = new LinkedHashMap<>();
        for (String name : List.of(
                "Q1A", "Q1B", "Q2A", "Q2B",
                "R1A", "R1B", "R2A", "R2B",
                "S1A", "S1B", "S1C", "S2A", "S2B", "S2C",
                "T1A", "T1B", "T1C", "T2A", "T2B", "T2C")) {
            leaves.put(name, simple(name));
        }
        Simple start = simple("start");
        Simple end = simple("end");

        Composite q1 = composite("Q1", leaves.get("Q1A"), leaves.get("Q1B"));
        Composite q2 = composite("Q2", leaves.get("Q2A"), leaves.get("Q2B"));
        q2.addPrecondition(q1);
        Composite q = composite("Q", q1, q2);

        // R1 represents the two XOR alternatives as descendants; buildWorkflow does not select a branch.
        Composite r1 = composite("R1", leaves.get("R1A"), leaves.get("R1B"));
        Composite r2 = composite("R2", leaves.get("R2A"), leaves.get("R2B"));
        leaves.get("R2B").addPrecondition(leaves.get("R2A"));
        Composite r = composite("R", r1, r2);

        Composite s1 = composite("S1", leaves.get("S1A"), leaves.get("S1B"), leaves.get("S1C"));
        leaves.get("S1B").addPrecondition(leaves.get("S1A"));
        leaves.get("S1C").addPrecondition(leaves.get("S1B"));
        Composite s2 = composite("S2", leaves.get("S2A"), leaves.get("S2B"), leaves.get("S2C"));
        leaves.get("S2B").addPrecondition(leaves.get("S2A"));
        leaves.get("S2C").addPrecondition(leaves.get("S2B"));
        Composite s = composite("S", s1, s2);

        Composite t1 = composite("T1", leaves.get("T1A"), leaves.get("T1B"), leaves.get("T1C"));
        Composite t2 = composite("T2", leaves.get("T2A"), leaves.get("T2B"), leaves.get("T2C"));
        t2.addPrecondition(t1);
        Composite t = composite("T", t1, t2);

        q.addPrecondition(start);
        r.addPrecondition(start);
        s.addPrecondition(q, r);
        t.addPrecondition(r);
        end.addPrecondition(s, t);
        Composite root = composite("TOP", start, q, r, s, t, end);

        Map<String, Composite> composites = new LinkedHashMap<>();
        composites.put("TOP", root);
        composites.put("Q", q);
        composites.put("Q1", q1);
        composites.put("Q2", q2);
        composites.put("R", r);
        composites.put("R1", r1);
        composites.put("R2", r2);
        composites.put("S", s);
        composites.put("S1", s1);
        composites.put("S2", s2);
        composites.put("T", t);
        composites.put("T1", t1);
        composites.put("T2", t2);
        return new PhotoWorkflow(root, start, end, leaves, composites);
    }

    private static Map<Activity, AbstractNode> assignments(PhotoWorkflow photo,
                                                            AbstractNode startNode,
                                                            AbstractNode qNode,
                                                            AbstractNode rNode,
                                                            AbstractNode sNode,
                                                            AbstractNode tNode,
                                                            AbstractNode endNode) {
        Map<Activity, AbstractNode> assignments = new LinkedHashMap<>();
        assignments.put(photo.start(), startNode);
        assignments.put(photo.end(), endNode);
        for (Map.Entry<String, Simple> entry : photo.leaves().entrySet()) {
            String name = entry.getKey();
            AbstractNode node = switch (name.charAt(0)) {
                case 'Q' -> qNode;
                case 'R' -> rNode;
                case 'S' -> sNode;
                case 'T' -> tNode;
                default -> throw new IllegalArgumentException("Modulo sconosciuto: " + name);
            };
            assignments.put(entry.getValue(), node);
        }
        return assignments;
    }

    private static List<Simple> generatedHops(Composite root) {
        return root.activities().stream()
                .filter(Simple.class::isInstance)
                .map(Simple.class::cast)
                .filter(activity -> activity.name().contains("_hop"))
                .toList();
    }

    private static void assertTransferChain(PhotoWorkflow photo,
                                            String sourceName,
                                            String targetName,
                                            StochasticTime firstDelay,
                                            StochasticTime secondDelay) {
        Simple source = photo.leaves().get(sourceName);
        Simple target = photo.leaves().get(targetName);
        Simple hop1 = hopNamed(photo, sourceName + "_" + targetName + "_hop1");
        Simple hop2 = hopNamed(photo, sourceName + "_" + targetName + "_hop2");

        assertSame(firstDelay, hop1.getPdf());
        assertSame(secondDelay, hop2.getPdf());
        assertEquals(List.of(source), hop1.pre());
        assertEquals(List.of(hop2), hop1.post());
        assertEquals(List.of(hop1), hop2.pre());
        assertEquals(List.of(target), hop2.post());
        assertTrue(source.post().contains(hop1));
        assertTrue(target.pre().contains(hop2));
    }

    private static void assertSingleTransfer(PhotoWorkflow photo,
                                             String sourceName,
                                             String targetName,
                                             StochasticTime delay) {
        Simple source = sourceName.equals("start") ? photo.start() : photo.leaves().get(sourceName);
        Simple target = targetName.equals("end") ? photo.end() : photo.leaves().get(targetName);
        Simple hop = hopNamed(photo, sourceName + "_" + targetName + "_hop1");

        assertSame(delay, hop.getPdf());
        assertEquals(List.of(source), hop.pre());
        assertEquals(List.of(target), hop.post());
        assertTrue(source.post().contains(hop));
        assertTrue(target.pre().contains(hop));
    }

    private static boolean hasHop(PhotoWorkflow photo, String sourceName, String targetName) {
        String expectedName = sourceName + "_" + targetName + "_hop1";
        return generatedHops(photo.root()).stream().anyMatch(hop -> hop.name().equals(expectedName));
    }

    private static boolean hasHopWithPdf(PhotoWorkflow photo, StochasticTime delay) {
        return generatedHops(photo.root()).stream().anyMatch(hop -> hop.getPdf() == delay);
    }

    private static Simple hopNamed(PhotoWorkflow photo, String name) {
        return generatedHops(photo.root()).stream()
                .filter(hop -> hop.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Manca la Simple di rete " + name));
    }

    private static ActivitySnapshot snapshot(Activity root) {
        List<Activity> activities = new ArrayList<>();
        collectTree(root, activities);
        Map<Activity, List<Activity>> preconditions = new IdentityHashMap<>();
        Map<Activity, List<Activity>> postconditions = new IdentityHashMap<>();
        Map<Activity, List<Activity>> children = new IdentityHashMap<>();
        for (Activity activity : activities) {
            preconditions.put(activity, List.copyOf(activity.pre()));
            postconditions.put(activity, List.copyOf(activity.post()));
            children.put(activity, activity.activities() == null ? null : List.copyOf(activity.activities()));
        }
        return new ActivitySnapshot(preconditions, postconditions, children);
    }

    private static void assertUnchanged(ActivitySnapshot snapshot) {
        snapshot.preconditions().forEach((activity, expected) -> assertEquals(expected, activity.pre()));
        snapshot.postconditions().forEach((activity, expected) -> assertEquals(expected, activity.post()));
        snapshot.children().forEach((activity, expected) -> assertEquals(expected, activity.activities()));
    }

    private static void collectTree(Activity activity, List<Activity> result) {
        result.add(activity);
        if (activity.activities() != null) {
            for (Activity child : activity.activities()) {
                collectTree(child, result);
            }
        }
    }

    private static Placement placement(Service service,
                                       BidirectionalWeightedGraph<AbstractNode> graph,
                                       Map<? extends Activity, ? extends AbstractNode> assignments) {
        Map<Activity, AbstractNode> copy = new LinkedHashMap<>();
        copy.putAll(assignments);
        return new Placement(service, copy, new NetworkGraph(graph));
    }

    private static Composite composite(Activity... children) {
        Composite root = new Composite();
        root.setActivities(List.of(children));
        return root;
    }

    private static Composite composite(String name, Activity... children) {
        Composite composite = composite(children);
        composite.setName(name);
        return composite;
    }

    private static Simple simple(String name) {
        return new Simple(name, new UniformTime(1, 1));
    }

    private static RospinoNode node(int id) {
        return new RospinoNode(id, 4);
    }

    private static BidirectionalWeightedGraph<AbstractNode> graphWith(AbstractNode... nodes) {
        BidirectionalWeightedGraph<AbstractNode> graph = new BidirectionalWeightedGraph<>();
        for (AbstractNode node : nodes) {
            graph.addNode(node);
        }
        return graph;
    }

    private record PhotoWorkflow(Composite root,
                                 Simple start,
                                 Simple end,
                                 Map<String, Simple> leaves,
                                 Map<String, Composite> composites) { }

    private record ActivitySnapshot(Map<Activity, List<Activity>> preconditions,
                                    Map<Activity, List<Activity>> postconditions,
                                    Map<Activity, List<Activity>> children) { }
}
