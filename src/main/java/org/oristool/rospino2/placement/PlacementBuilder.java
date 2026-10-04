package org.oristool.rospino2.placement;

import org.oristool.eulero.modeling.Activity;
import org.oristool.rospino2.computingNodes.NetworkGraph;

public abstract class PlacementBuilder {
    private NetworkGraph networkGraph;
    private Activity workflow;


    public NetworkGraph getNetworkGraph() {
        return networkGraph;
    }

    public PlacementBuilder setNetworkGraph(NetworkGraph networkGraph) {
        this.networkGraph = networkGraph;
        return this;
    }

    public Activity getWorkflow() {
        return workflow;
    }

    public PlacementBuilder setWorkflow(Activity workflow) {
        this.workflow = workflow;
        return this;
    }

    public abstract Placement build();
}