package org.oristool.rospino2.placement;

import org.oristool.eulero.modeling.Activity;
import org.oristool.eulero.modeling.Simple;
import org.oristool.eulero.modeling.stochastictime.StochasticTime;
import org.oristool.rospino2.computingNodes.AbstractNode;
import org.oristool.rospino2.computingNodes.BidirectionalWeightedGraph;
import org.oristool.rospino2.computingNodes.NetworkGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Questa entità effettua il placement del workflow Eulero sulla rete descritta da {@code networkGraph}.
 * La mappa {@code assignements} memorizza l'associazione tra l'attività del workflow e il nodo a cui è stata assegnata.
 */
public class Placement {

    /**
     * Mappa con come chiavi le attività e come valori i nodi a cui sono assegnate.
     */
    private final Map<Activity, AbstractNode> assignments;
    private final Service workflow;
    private final NetworkGraph networkGraph;
    private boolean workflowBuilt;

    public Placement(Service workflow, Map<Activity, AbstractNode> assignments, NetworkGraph networkGraph) {
        this.assignments = assignments;
        this.networkGraph = networkGraph;
        this.workflow = workflow;
    }

    public Service getWorkflow() {
        return workflow;
    }

    public Map<Activity, AbstractNode> getAssignments() {
        return assignments;
    }

    public NetworkGraph getNetworkGraph() {
        return networkGraph;
    }

    /** Inserts one Simple for every network edge crossed by each workflow dependency. */
    public Activity buildWorkflow() {
        Activity root = Objects.requireNonNull(workflow, "Workflow mancante").getActivity();
        if (workflowBuilt) {
            return root;
        }
        BidirectionalWeightedGraph<AbstractNode> graph =
                Objects.requireNonNull(networkGraph, "Grafo di rete mancante").getAdjMatrix();

        List<Activity> activities = new ArrayList<>();
        Set<Activity> included = Collections.newSetFromMap(new IdentityHashMap<>());
        collectActivities(root, included, activities);

        Map<Activity, List<Simple>> descendants = new IdentityHashMap<>();
        for (Activity activity : activities) {
            List<Simple> simples = new ArrayList<>();
            collectSimples(activity, Collections.newSetFromMap(new IdentityHashMap<>()), simples);
            descendants.put(activity, simples);
        }

        for (Simple simple : descendants.get(root)) {
            AbstractNode node = assignments.get(simple);
            if (node == null || !graph.hasNode(node)) {
                throw new IllegalStateException("Assegnazione mancante o nodo sconosciuto per: " + simple.name());
            }
        }

        Set<Dependency> dependencies = new LinkedHashSet<>();
        for (Activity activity : activities) {
            for (Activity predecessor : activity.pre()) {
                if (!included.contains(predecessor)) {
                    throw new IllegalArgumentException("Precondition esterna al Service: " + predecessor);
                }
                for (Simple target : descendants.get(activity)) {
                    for (Simple source : descendants.get(predecessor)) {
                        if (source == target) {
                            throw new IllegalArgumentException("Dipendenza circolare per: " + source.name());
                        }
                        dependencies.add(new Dependency(source, target));
                    }
                }
            }
        }

        List<Transfer> transfers = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            AbstractNode sourceNode = assignments.get(dependency.source());
            AbstractNode targetNode = assignments.get(dependency.target());
            if (sourceNode.equals(targetNode)) {
                continue;
            }
            List<AbstractNode> path = graph.shortestPath(sourceNode, targetNode)
                    .orElseThrow(() -> new IllegalStateException(
                            "Nessun percorso tra " + dependency.source().name() + " e " + dependency.target().name()));
            List<StochasticTime> weights = new ArrayList<>(path.size() - 1);
            for (int i = 1; i < path.size(); i++) {
                weights.add(graph.getWeight(path.get(i - 1), path.get(i))
                        .orElseThrow(() -> new IllegalStateException("Percorso di rete senza peso")));
            }
            transfers.add(new Transfer(dependency.source(), dependency.target(), weights));
        }

        if (!transfers.isEmpty()) {
            List<Activity> children = root.activities() == null
                    ? new ArrayList<>() : new ArrayList<>(root.activities());
            Set<String> names = new LinkedHashSet<>();
            for (Activity activity : activities) {
                names.add(activity.name());
            }
            for (Transfer transfer : transfers) {
                Activity previous = transfer.source();
                if (transfer.target().pre().contains(transfer.source())) {
                    transfer.target().removePrecondition(transfer.source());
                }
                int hopIndex = 1;
                for (StochasticTime weight : transfer.weights()) {
                    String baseName = transfer.source().name() + "_" + transfer.target().name()
                            + "_hop" + hopIndex++;
                    String name = baseName;
                    int suffix = 2;
                    while (!names.add(name)) {
                        name = baseName + "_" + suffix++;
                    }
                    Simple hop = new Simple(name, weight);
                    hop.addPrecondition(previous);
                    children.add(hop);
                    previous = hop;
                }
                transfer.target().addPrecondition(previous);
            }
            root.setActivities(children);
        }
        workflowBuilt = true;
        return root;
    }

    private void collectActivities(Activity activity, Set<Activity> visited, List<Activity> result) {
        if (activity == null || !visited.add(activity)) {
            return;
        }
        result.add(activity);
        if (activity.activities() != null) {
            for (Activity child : activity.activities()) {
                collectActivities(child, visited, result);
            }
        }
    }

    private void collectSimples(Activity activity, Set<Activity> visited, List<Simple> result) {
        if (activity == null || !visited.add(activity)) {
            return;
        }
        if (activity instanceof Simple simple) {
            result.add(simple);
        }
        if (activity.activities() != null) {
            for (Activity child : activity.activities()) {
                collectSimples(child, visited, result);
            }
        }
    }

    private record Dependency(Simple source, Simple target) { }

    private record Transfer(Simple source, Simple target, List<StochasticTime> weights) { }
}
