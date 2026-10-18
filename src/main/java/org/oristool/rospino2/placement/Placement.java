package org.oristool.rospino2.placement;

import org.oristool.eulero.modeling.Activity;
import org.oristool.eulero.modeling.Simple;
import org.oristool.eulero.modeling.stochastictime.StochasticTime;
import org.oristool.rospino2.computingNodes.AbstractNode;
import org.oristool.rospino2.computingNodes.NetworkGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    /**
     * Restituisce un oggetto {@code Activity} che identifica ogni salto tra nodi della rete come delle Activity di tipo {@code Simple}
     *
     * @return Activity
     */
    public Activity buildWorkflow() {
        ArrayList<Activity> activities = new ArrayList<>(assignments.keySet());
        Collections.reverse(activities);
        for (Activity activity : activities) {
            AbstractNode assignment = assignments.get(activity);
            List<Simple> newPreconditions = new ArrayList<>();
            //Scorro le precondizioni dell'attività in analisi e, per ognuna, costruisco un Simple che ha come precondizione la precondizione dell'attività in analisi
            for (Activity predecessor : new ArrayList<>(activity.pre())) {
                AbstractNode predecessorAssignment = assignments.get(predecessor);
                StochasticTime delayBetweenNodes = networkGraph.getDelayBetweenNodes(assignment, predecessorAssignment);
                Simple simple = new Simple(activity.name() + "_" + predecessor.name(), delayBetweenNodes);
                activity.removePrecondition(predecessor);
                simple.addPrecondition(predecessor);
                newPreconditions.add(simple);
            }
            newPreconditions.forEach(activity::addPrecondition);
        }
        //Sono state analizzare al contrario, quindi l'ultima attività è la prima del workflow
        return activities.getLast();
    }

}
