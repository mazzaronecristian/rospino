package org.oristool.rospino2;

import com.google.common.collect.Lists;
//import org.oristool.eulero.modeling.deprecated.Activity;
//import org.oristool.eulero.modeling.deprecated.DAG;
//import org.oristool.eulero.modeling.deprecated.Simple;
//import org.oristool.eulero.modeling.deprecated.XOR;
import org.oristool.eulero.modeling.Activity;
import org.oristool.eulero.modeling.Composite;
import org.oristool.eulero.modeling.Simple;
import org.oristool.eulero.modeling.activitytypes.ActivityEnumType;
import org.oristool.eulero.modeling.activitytypes.ActivityType;
import org.oristool.eulero.modeling.activitytypes.DAGType;
import org.oristool.eulero.modeling.activitytypes.SEQType;
import org.oristool.eulero.modeling.stochastictime.StochasticTime;
import org.oristool.eulero.modeling.stochastictime.UniformTime;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.rospino.placement.DeploymentManager;
import org.oristool.rospino.placement.DeploymentPlan;
import org.oristool.rospino.placement.Service;
import org.oristool.rospino2.computingNodes.AbstractNode;
import org.oristool.rospino2.computingNodes.StronglyConnectedWeightedGraph;
import org.oristool.rospino2.computingNodes.Util;
import org.oristool.rospino2.placement.PlacementBuilder;
import org.oristool.rospino2.placement.PlacementManager;
import org.oristool.rospino2.placement.Rospino2Node;
import org.oristool.rospino2.placement.SimplePlacementBuilder;

import java.util.List;

import static org.oristool.models.stpn.trees.StochasticTransitionFeature.newUniformInstance;

public class Main {

    public static void main(String[] args) {

    }
}