package org.oristool.rospino.placement;

import org.oristool.rospino.computingNodes.Delay;
import org.oristool.rospino.computingNodes.NetworkGraph;
import org.oristool.rospino.computingNodes.Node;

import java.math.BigDecimal;
import java.util.Map;

public class DeploymentPlan {
    /**
     * Mappa con come chiave i servizi e come valori i nodi a cui il servizio è assegnato
     */
    private final Map<Service, Node> assignments;
    private final NetworkGraph networkGraph;

    public DeploymentPlan(Map<Service, Node> assignments, NetworkGraph networkGraph) {
        this.assignments = assignments;
        this.networkGraph = networkGraph;
    }

    public Map<Service, Node> getAssignments() {
        return assignments;
    }

    public NetworkGraph getNetworkGraph() {
        return networkGraph;
    }

    /**
     * Costo totale del placement corrente (per valutazione/debug).
     */
    public BigDecimal totalCost() {
        BigDecimal total = BigDecimal.valueOf(0.0);
        for (Service service : assignments.keySet()) {
            total = total.add(BigDecimal.valueOf(service.responseTime()));
            Node toNode = assignments.get(service);
            if (toNode == null) continue;
            for (Service predecessor : service.getPredecessors()) {
                Node fromNode = assignments.get(predecessor);
                if (fromNode == null) continue;
                Delay delayBetweenNodes = networkGraph.getDelayBetweenNodes(fromNode, toNode);
                total = total.add(delayBetweenNodes.getLft());
            }
        }
        return total;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        assignments.forEach((svc, node) ->
                stringBuilder.append("Servizio ")
                        .append(svc)
                        .append(" -> ")
                        .append(node.getId())
                        .append("\n")
        );
        return stringBuilder.toString();
    }
}