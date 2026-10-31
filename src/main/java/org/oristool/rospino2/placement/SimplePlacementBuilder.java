package org.oristool.rospino2.placement;

import org.oristool.eulero.modeling.Activity;
import org.oristool.eulero.modeling.Simple;
import org.oristool.eulero.modeling.stochastictime.StochasticTime;
import org.oristool.rospino2.computingNodes.AbstractNode;
import org.oristool.rospino2.computingNodes.BidirectionalWeightedGraph;
import org.oristool.rospino2.computingNodes.NetworkGraph;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

public class SimplePlacementBuilder extends PlacementBuilder {
    @Override
    public Placement build() {
        Service service = Objects.requireNonNull(getWorkflow(), "Workflow mancante");
        NetworkGraph network = Objects.requireNonNull(getNetworkGraph(), "Grafo di rete mancante");
        BidirectionalWeightedGraph<AbstractNode> graph = network.getAdjMatrix();

        List<Activity> sortedActivities = topologicalSort(service);
        Map<Simple, Set<Simple>> predecessors = dependencies(service);
        Map<Activity, AbstractNode> assignments = new LinkedHashMap<>();
        Map<AbstractNode, Double> remainingCores = new LinkedHashMap<>();
        for (AbstractNode node : graph.nodes()) {
            remainingCores.put(node, node.getAvailableCores());
        }

        for (Activity activity : sortedActivities) {
            Simple simple = (Simple) activity;
            int demand = service.getResourcesPerSimple().get(simple);
            AbstractNode bestNode = null;
            BigDecimal bestCost = null;

            for (Map.Entry<AbstractNode, Double> entry : remainingCores.entrySet()) {
                AbstractNode candidate = entry.getKey();
                if (entry.getValue() < demand || !candidate.canHost(demand)) {
                    continue;
                }

                Optional<BigDecimal> cost = delayCost(predecessors.get(simple), assignments, candidate, graph);
                if (cost.isPresent() && (bestCost == null || cost.get().compareTo(bestCost) < 0)) {
                    bestCost = cost.get();
                    bestNode = candidate;
                }
            }

            if (bestNode == null) {
                throw new IllegalStateException("Placement impossibile per la Simple: " + simple.name());
            }
            assignments.put(simple, bestNode);
            remainingCores.put(bestNode, remainingCores.get(bestNode) - demand);
        }

        // Commit resource reservations only after every Simple has a feasible assignment.
        for (Map.Entry<AbstractNode, Double> entry : remainingCores.entrySet()) {
            AbstractNode node = entry.getKey();
            if (Double.compare(node.getAvailableCores(), entry.getValue()) != 0) {
                node.setAvailableCores(entry.getValue());
            }
        }
        return new Placement(service, assignments, network);
    }

    /** Returns all Simple activities in topological order, using service traversal order for ties. */
    public List<Activity> topologicalSort(Service service) {
        Map<Simple, Set<Simple>> predecessors = dependencies(service);
        Map<Simple, Integer> visitOrder = new IdentityHashMap<>();
        Map<Simple, Integer> inDegree = new IdentityHashMap<>();
        Map<Simple, List<Simple>> successors = new IdentityHashMap<>();
        int index = 0;
        for (Simple simple : predecessors.keySet()) {
            visitOrder.put(simple, index++);
            inDegree.put(simple, predecessors.get(simple).size());
            successors.put(simple, new ArrayList<>());
        }
        for (Map.Entry<Simple, Set<Simple>> entry : predecessors.entrySet()) {
            for (Simple predecessor : entry.getValue()) {
                successors.get(predecessor).add(entry.getKey());
            }
        }

        PriorityQueue<Simple> ready = new PriorityQueue<>(Comparator.comparingInt(visitOrder::get));
        for (Simple simple : predecessors.keySet()) {
            if (inDegree.get(simple) == 0) {
                ready.add(simple);
            }
        }

        List<Activity> ordered = new ArrayList<>(predecessors.size());
        while (!ready.isEmpty()) {
            Simple current = ready.remove();
            ordered.add(current);
            for (Simple successor : successors.get(current)) {
                int degree = inDegree.merge(successor, -1, Integer::sum);
                if (degree == 0) {
                    ready.add(successor);
                }
            }
        }
        if (ordered.size() != predecessors.size()) {
            throw new IllegalArgumentException("Il workflow contiene un ciclo di precondition");
        }
        return ordered;
    }

    private Map<Simple, Set<Simple>> dependencies(Service service) {
        Objects.requireNonNull(service, "Service mancante");
        Map<Simple, Set<Simple>> predecessors = new LinkedHashMap<>();
        for (Simple simple : service.getResourcesPerSimple().keySet()) {
            predecessors.put(simple, new LinkedHashSet<>());
        }

        List<Activity> activities = new ArrayList<>();
        Set<Activity> included = Collections.newSetFromMap(new IdentityHashMap<>());
        collectActivities(service.getActivity(), included, activities);

        Map<Activity, List<Simple>> descendants = new IdentityHashMap<>();
        for (Activity activity : activities) {
            List<Simple> simples = new ArrayList<>();
            collectDescendants(activity, Collections.newSetFromMap(new IdentityHashMap<>()), simples);
            descendants.put(activity, simples);
        }

        for (Activity activity : activities) {
            for (Activity predecessor : activity.pre()) {
                if (!included.contains(predecessor)) {
                    throw new IllegalArgumentException("Precondition esterna al Service: " + predecessor);
                }
                for (Simple dependent : descendants.get(activity)) {
                    for (Simple prerequisite : descendants.get(predecessor)) {
                        predecessors.get(dependent).add(prerequisite);
                    }
                }
            }
        }
        return predecessors;
    }

    private void collectActivities(Activity activity, Set<Activity> visited, List<Activity> result) {
        if (activity == null || !visited.add(activity)) {
            return;
        }
        result.add(activity);
        List<Activity> children = activity.activities();
        if (children != null) {
            for (Activity child : children) {
                collectActivities(child, visited, result);
            }
        }
    }

    private void collectDescendants(Activity activity, Set<Activity> visited, List<Simple> result) {
        if (activity == null || !visited.add(activity)) {
            return;
        }
        if (activity instanceof Simple simple) {
            result.add(simple);
        }
        List<Activity> children = activity.activities();
        if (children != null) {
            for (Activity child : children) {
                collectDescendants(child, visited, result);
            }
        }
    }

    private Optional<BigDecimal> delayCost(Set<Simple> predecessors,
                                           Map<Activity, AbstractNode> assignments,
                                           AbstractNode candidate,
                                           BidirectionalWeightedGraph<AbstractNode> graph) {
        BigDecimal total = BigDecimal.ZERO;
        for (Simple predecessor : predecessors) {
            AbstractNode source = assignments.get(predecessor);
            if (source == null) {
                throw new IllegalStateException("Predecessore non assegnato: " + predecessor.name());
            }
            if (source.equals(candidate)) {
                continue;
            }

            Optional<List<AbstractNode>> path = graph.shortestPath(source, candidate);
            if (path.isEmpty()) {
                return Optional.empty();
            }
            List<AbstractNode> nodes = path.get();
            for (int i = 1; i < nodes.size(); i++) {
                StochasticTime weight = graph.getWeight(nodes.get(i - 1), nodes.get(i))
                        .orElseThrow(() -> new IllegalStateException("Percorso senza peso"));
                total = total.add(weight.getLFT());
            }
        }
        return Optional.of(total);
    }
}
