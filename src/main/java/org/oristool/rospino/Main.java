package org.oristool.rospino;

import com.google.common.collect.Lists;
import org.oristool.eulero.modeling.deprecated.Activity;
import org.oristool.eulero.modeling.deprecated.Simple;
import org.oristool.eulero.modeling.deprecated.DAG;
import org.oristool.eulero.modeling.deprecated.XOR;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.rospino.computingNodes.Delay;
import org.oristool.rospino.computingNodes.Node;
import org.oristool.rospino.computingNodes.WeightedGraph;
import org.oristool.rospino.placement.DeploymentManager;
import org.oristool.rospino.placement.Service;

import java.util.*;

import static org.oristool.models.stpn.trees.StochasticTransitionFeature.newUniformInstance;

public class Main {
    public static void main(String[] args) {
        // Rete: 2 nodi, ciascuno con 4 core
        Node n1 = new Node(1, 4);
        Node n2 = new Node(2, 4);
        Node n3 = new Node(3, 4);

        WeightedGraph<Node, Delay> weightedGraph = new WeightedGraph<>();
        weightedGraph.addNode(n1,new Delay(0,0));
        weightedGraph.addNode(n2,new Delay(0,0));
        weightedGraph.addNode(n3,new Delay(0,0));
        weightedGraph.addEdge(n1, n2, new Delay(1,5));
        weightedGraph.addEdge(n1, n3, new Delay(1,5));
        weightedGraph.addEdge(n2, n3, new Delay(1,5));

        // Servizi: id, jobSize, ioTime, demand

        StochasticTransitionFeature feature = newUniformInstance( "0" , "1" ) ;;

        Activity Q = DAG. sequence ( "Q" ,
                DAG.forkJoin( "Q1" ,
                        new Simple( "Q1A" , feature ) ,
                        new Simple ( "Q1B" , feature )

                ) ,
                DAG.forkJoin( "Q2" ,
                        new Simple ( "Q2A" , feature ) ,
                        new Simple ( "Q2B" , feature )
                )
        ) ;

        Activity R = DAG . forkJoin ( "R" ,
                new XOR( "R1" ,
                        List . of (
                                new Simple ( "R1A" , feature ) ,
                                new Simple ( "R1b" , feature )
                        ) ,
                        List . of (0.3 , 0.7) ) ,
                DAG . sequence ( "R2" ,
                        new Simple ( "R2A" , feature ) ,
                        new Simple ( "R2B" , feature )
                )
        ) ;

        Activity S = DAG . forkJoin ( "S" ,
                DAG . sequence ( "S1" ,
                        new Simple ( "S1A" , feature ) ,
                        new Simple ( "S1B" , feature ) ,
                        new Simple ( "S1C" , feature )
                ) ,
                DAG . sequence ( "S2" ,
                        new Simple ( "S2A" , feature ) ,
                        new Simple ( "S2B" , feature ) ,
                        new Simple ( "S2C" , feature )
                )
        ) ;

        DAG T = DAG . sequence ( "T" ,
                DAG . forkJoin ( "T1" ,
                        new Simple ( "T1A" , feature ) ,
                        new Simple ( "T1B" , feature ) ,
                        new Simple ( "T1C" , feature )
                ) ,
                DAG . forkJoin ( "T2" ,
                        new Simple ( "T2A" , feature ) ,
                        new Simple ( "T2B" , feature ) ,
                        new Simple ( "T2C" , feature )
                )
        ) ;

        DAG top = DAG . empty ( "TOP" ) ;
        Q.addPrecondition( top . begin () ) ;
        R.addPrecondition( top . begin () ) ;
        T.addPrecondition( R ) ;
        S.addPrecondition(R , Q ) ;
        top.end().addPrecondition(T , S ) ;
        top.setMin( top.getMinBound( top.end () ) ) ;
        top.setMax ( top . getMaxBound ( top . end () ) ) ;
        top.setActivities ( Lists. newArrayList (Q , R , S , T ) ) ;


        Service sA = new Service(top, 4, 2, 2, "A");
        Service sB = new Service(top, 3,  1, 1, "B");
        Service sC = new Service(top, 2,  3, 2, "C");
        List<Service> services = List.of(sA, sB, sC);


        DeploymentManager planner = new DeploymentManager(weightedGraph);
        // DAG: A → B → C
        sB.addPredecessors(sA);
        sC.addPredecessors(sB);

        Map<Service, Node> result = planner.place(services);
        result.forEach((svc, node) ->
                System.out.println("Servizio " + svc + " → " + node.getId()));

        System.out.printf("Costo totale: %.2f%n", planner.totalCost(services));
    }
}


