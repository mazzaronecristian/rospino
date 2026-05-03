package org.oristool.rospino.placement;

import org.oristool.rospino.computingNodes.Delay;
import org.oristool.rospino.computingNodes.NetworkGraph;
import org.oristool.rospino.computingNodes.Node;

import java.math.BigDecimal;
import java.util.*;

public class DeploymentManager implements IDeploymentManager {
    private static DeploymentManager instance;

    public static synchronized DeploymentManager getInstance() {
        if (instance == null) {
            instance = new DeploymentManager();
        }
        return instance;
    }

    /**
     * Esegue il placement greedy.
     * Per ogni servizio (in ordine topologico) sceglie il nodo che minimizza
     * la somma dei delay verso i predecessori già piazzati, prediligendo i nodi con meno cpu libere.
     * Si prediligono i nodi con cpu libere in quanto un servizi non è divisibile su più nodi; quindi se su un nodi rimangono meno cpu è più probabile che non verranno utilizzate
     */
    @Override
    public DeploymentPlan place(NetworkGraph networkGraph, List<Service> services) {
        List<Service> sorted = topologicalSort(services);
        Map<Service, Node> assignments = new HashMap<>();
        for (Service service : sorted) {
            Node best = null;
            BigDecimal bestCost = BigDecimal.valueOf(Double.MAX_VALUE);
            Set<Node> nodes = networkGraph.getNodes();
            Comparator<Node> byAvailableCores = Comparator.comparingDouble(Node::getAvailableCores);
            for (Node candidate : nodes.stream().sorted(byAvailableCores).toList()) {
                if (!candidate.canHost(service)) continue;
                BigDecimal cost = delayCost(service, assignments, networkGraph, candidate);
                if (cost.compareTo(bestCost) < 0) {
                    bestCost = cost;
                    best = candidate;
                }
            }

            if (best == null)
                throw new IllegalStateException(
                        "Placement impossibile per il servizio: " + service.getId());

            best.allocate(service);
            assignments.put(service, best);
        }

        return new DeploymentPlan(assignments, networkGraph);
    }

    /**
     * Costo di mettere 'service' su 'candidate':
     * somma dei delay dal nodo di ogni predecessore al nodo candidate.
     * Se un predecessore non è ancora piazzato (non dovrebbe accadere con
     * ordine topologico), il suo contributo è 0.
     */
    private BigDecimal delayCost(Service service, Map<Service, Node> assignments, NetworkGraph networkGraph, Node candidate) {
        Set<Service> predecessors = service.getPredecessors();
        BigDecimal cost = BigDecimal.valueOf(0.0);

        for (Service predecessor : predecessors) {
            Node predNode = assignments.get(predecessor);
            if (predNode == null) continue;
            Delay delayBetweenNodes = networkGraph.getDelayBetweenNodes(predNode, candidate);
            cost = cost.add(delayBetweenNodes.getLft());
        }
        return cost;
    }

    /**
     * Kahn's algorithm per il topological sort.
     */
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
                successorsPerService.computeIfAbsent(predecessor, _ -> new ArrayList<>()).add(service);
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

}