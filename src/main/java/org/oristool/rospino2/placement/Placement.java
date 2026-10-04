package org.oristool.rospino2.placement;

import org.oristool.eulero.modeling.Activity;
import org.oristool.rospino2.computingNodes.AbstractNode;
import org.oristool.rospino2.computingNodes.NetworkGraph;

import java.util.Map;

/**
 * Questa entità effettua il placement del workflow Eulero sulla rete descritta da {@code networkGraph}.
 * La mappa {@code assignements} memorizza l'associazione tra l'attività del workflow e il nodo a cui è stata assegnata.
 */
public class Placement {

    /**
     * Mappa con come chiave i servizi e come valori i nodi a cui il servizio è assegnato
     */
    private final Map<Activity, AbstractNode> assignments;
    private final NetworkGraph networkGraph;

    public Placement(Map<Activity, AbstractNode> assignments, NetworkGraph networkGraph) {
        this.assignments = assignments;
        this.networkGraph = networkGraph;
    }

    public Map<Activity, AbstractNode> getAssignments() {
        return assignments;
    }

    public NetworkGraph getNetworkGraph() {
        return networkGraph;
    }
}