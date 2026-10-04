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
        // Rete: 2 nodi, ciascuno con 4 core
        Rospino2Node n1 = new Rospino2Node(1, 4);
        Rospino2Node n2 = new Rospino2Node(2, 4);
        Rospino2Node n3 = new Rospino2Node(3, 4);

        StronglyConnectedWeightedGraph<AbstractNode, StochasticTime> stronglyConnectedWeightedGraph = new StronglyConnectedWeightedGraph<>();
        stronglyConnectedWeightedGraph.addNode(n1, Util.getUniformZero());
        stronglyConnectedWeightedGraph.addNode(n2, Util.getUniformZero());
        stronglyConnectedWeightedGraph.addNode(n3, Util.getUniformZero());
        stronglyConnectedWeightedGraph.addEdge(n1, n2, new UniformTime(1, 5));
        stronglyConnectedWeightedGraph.addEdge(n1, n3, new UniformTime(1, 5));
        stronglyConnectedWeightedGraph.addEdge(n2, n3, new UniformTime(1, 5));
        org.oristool.rospino2.computingNodes.NetworkGraph networkGraph = new org.oristool.rospino2.computingNodes.NetworkGraph(stronglyConnectedWeightedGraph);

        // Servizi: id, jobSize, ioTime, demand

        StochasticTransitionFeature feature = newUniformInstance("0", "1");

        Activity Q = DAG.sequence("Q",
                DAG.forkJoin("Q1",
                        new Simple("Q1A", feature),
                        new Simple("Q1B", feature)

                ),
                DAG.forkJoin("Q2",
                        new Simple("Q2A", feature),
                        new Simple("Q2B", feature)
                )
        );

        Activity R = DAG.forkJoin("R",
                new XOR("R1",
                        List.of(
                                new Simple("R1A", feature),
                                new Simple("R1b", feature)
                        ),
                        List.of(0.3, 0.7)),
                DAG.sequence("R2",
                        new Simple("R2A", feature),
                        new Simple("R2B", feature)
                )
        );

        Activity S = DAG.forkJoin("S",
                DAG.sequence("S1",
                        new Simple("S1A", feature),
                        new Simple("S1B", feature),
                        new Simple("S1C", feature)
                ),
                DAG.sequence("S2",
                        new Simple("S2A", feature),
                        new Simple("S2B", feature),
                        new Simple("S2C", feature)
                )
        );

        DAG T = DAG.sequence("T",
                DAG.forkJoin("T1",
                        new Simple("T1A", feature),
                        new Simple("T1B", feature),
                        new Simple("T1C", feature)
                ),
                DAG.forkJoin("T2",
                        new Simple("T2A", feature),
                        new Simple("T2B", feature),
                        new Simple("T2C", feature)
                )
        );

        Simple top = new Simple();
        top.setName("TOP");
        Q.addPrecondition(top);
        R.addPrecondition(top);
        T.addPrecondition(R);
        S.addPrecondition(R, Q);
        top.end().addPrecondition(T, S);
        top.setMin(top.getMinBound(top.end()));
        top.setMax(top.getMaxBound(top.end()));
        top.setActivities(Lists.newArrayList(Q, R, S, T));


        Service sA = new Service(top, 4, 2, 2, "A");
        Service sB = new Service(top, 3, 1, 1, "B");
        Service sC = new Service(top, 2, 3, 2, "C");
        List<Service> services = List.of(sA, sB, sC);

        PlacementManager manager = new PlacementManager();
        PlacementBuilder builder = new SimplePlacementBuilder();
        builder.setNetworkGraph(networkGraph);
        builder.setWorkflow(top);
        manager.constructPlacement()

        DeploymentManager planner = DeploymentManager.getInstance();
        // DAG: A → B → C
        sB.addPredecessors(sA);
        sC.addPredecessors(sB);

        DeploymentPlan plan = planner.place(networkGraph, services);
        System.out.print(plan.toString());
        System.out.printf("Costo totale: %.2f%n%n", plan.totalCost());
    }
}