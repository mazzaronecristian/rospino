package org.oristool.rospino2.placement;

import org.oristool.eulero.modeling.Activity;
import org.oristool.rospino2.computingNodes.NetworkGraph;

public abstract class PlacementBuilder {
    private NetworkGraph networkGraph;
    private Service workflow;


    public NetworkGraph getNetworkGraph() {
        return networkGraph;
    }

    public PlacementBuilder setNetworkGraph(NetworkGraph networkGraph) {
        this.networkGraph = networkGraph;
        return this;
    }

    public Service getWorkflow() {
        return workflow;
    }

    public PlacementBuilder setWorkflow(Service workflow) {
        this.workflow = workflow;
        return this;
    }

    public abstract Placement build();
}