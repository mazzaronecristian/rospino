package org.oristool.rospino.placement;

import org.oristool.rospino.computingNodes.Delay;
import org.oristool.rospino.computingNodes.NetworkGraph;
import org.oristool.rospino.computingNodes.Node;
import org.oristool.rospino.computingNodes.WeightedGraph;

import java.math.BigDecimal;
import java.util.*;

public class DeploymentPlanner {
    private final NetworkGraph networkGraph;
    /**
     * Matrice di delay: delay[i][j] = ritardo tra nodo i e nodo j.
     * delay[i][i] = 0 (stesso nodo, nessun ritardo).
     * I nodi sono indicizzati nell'ordine della lista nodes.
     */
    private final Map<Integer, Integer> nodeIndex;

    // Grafo delle dipendenze tra servizi: edges(a) = predecessori di a
    private final Map<String, List<String>> predecessors;

    // Risultato del placement: servizio → nodo
    private final Map<Service, Node> assignment;

    public DeploymentPlanner(WeightedGraph<Node, Delay> weightedGraph) {
        this.nodeIndex = new HashMap<>();
        this.predecessors = new HashMap<>();
        this.assignment = new HashMap<>();
        this.networkGraph = new NetworkGraph(weightedGraph);
        List<Node> nodes = networkGraph.getNodes().stream().toList();
        for (int i = 0; i < nodes.size(); i++)
            nodeIndex.put(nodes.get(i).getId(), i);
    }

    /**
     * Esegue il placement greedy.
     * Per ogni servizio (in ordine topologico) sceglie il nodo che minimizza
     * la somma dei delay verso i predecessori già piazzati.
     */
    public Map<Service, Node> place(List<Service> services) {
        List<Service> sorted = topologicalSort(services);

        for (Service service : sorted) {
            Node best = null;
            BigDecimal bestCost = BigDecimal.valueOf(Double.MAX_VALUE);

            for (Node candidate : networkGraph.getNodes()) {
                if (!candidate.canHost(service)) continue;

                BigDecimal cost = delayCost(service, candidate);
                if (cost.compareTo(bestCost) < 0) {
                    bestCost = cost;
                    best = candidate;
                }
            }

            if (best == null)
                throw new IllegalStateException(
                        "Placement impossibile per il servizio: " + service.getId());

            best.allocate(service);
            assignment.put(service, best);
        }

        return Collections.unmodifiableMap(assignment);
    }

    /**
     * Costo di mettere 'service' su 'candidate':
     * somma dei delay dal nodo di ogni predecessore al nodo candidate.
     * Se un predecessore non è ancora piazzato (non dovrebbe accadere con
     * ordine topologico), il suo contributo è 0.
     */
    private BigDecimal delayCost(Service service, Node candidate) {
        Set<Service> predecessors = service.getPredecessors();
        BigDecimal cost = BigDecimal.valueOf(0.0);

        for (Service predecessor : predecessors) {
            Node predNode = assignment.get(predecessor);
            if (predNode == null) continue;
            Delay delayBetweenNodes = networkGraph.getDelayBetweenNodes(predNode, candidate);
            cost = cost.add(delayBetweenNodes.getLft());
        }
        return cost;
    }

    /** Kahn's algorithm per il topological sort. */
    private List<Service> topologicalSort(List<Service> services) {

        Map<Service, Integer> inDegree = new HashMap<>();
        for (Service service : services) inDegree.put(service, 0);
        for (Service service : services) {
            int numOfPredecessors = service.getPredecessors().size();
            inDegree.put(service, numOfPredecessors);
        }

        Map<Service, List<Service>> successorsPerService = new HashMap<>();
        for (Service service : services) {
            for (Service predecessor : service.getPredecessors()) {
                successorsPerService.computeIfAbsent(predecessor, s -> new ArrayList<>()).add(service);
            }

        }

        Queue<Service> queue = new ArrayDeque<>();
        for (Map.Entry<Service, Integer> e : inDegree.entrySet())
            if (e.getValue() == 0) queue.add(e.getKey());

        List<Service> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            Service service = queue.poll();
            result.add(service);
            for (Service successor : successorsPerService.getOrDefault(service, List.of())) {
                int deg = inDegree.merge(successor, -1, Integer::sum);
                if (deg == 0) queue.add(successor);
            }
        }

        if (result.size() != services.size())
            throw new IllegalArgumentException("Il DAG contiene un ciclo.");
        return result;
    }

    /** Costo totale del placement corrente (per valutazione/debug). */
    public BigDecimal totalCost(List<Service> services) {
        BigDecimal total = BigDecimal.valueOf(0.0);
        for (Service s : services)
            total = total.add(BigDecimal.valueOf(s.responseTime()));
        for (Map.Entry<String, List<String>> stringListEntry : predecessors.entrySet()) {
            Node toNode = assignment.get(stringListEntry.getKey());
            if (toNode == null) continue;
            for (String predId : stringListEntry.getValue()) {
                Node fromNode = assignment.get(predId);
                if (fromNode == null) continue;
                Delay delayBetweenNodes = networkGraph.getDelayBetweenNodes(fromNode, toNode);
                total = total.add(delayBetweenNodes.getLft());
            }
        }
        return total;
    }
}