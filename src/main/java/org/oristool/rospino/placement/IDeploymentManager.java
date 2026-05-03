package org.oristool.rospino.placement;

import org.oristool.rospino.computingNodes.NetworkGraph;

import java.util.List;

public interface IDeploymentManager {
    DeploymentPlan place(NetworkGraph networkGraph, List<Service> services);
}