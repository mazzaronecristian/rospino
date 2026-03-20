package org.oristool.rospino.computingNodes;

import java.util.Set;

public class NetworkGraph {
    //TODO: Definire un'interfaccia
    private final WeightedGraph<Node, Delay> adjMatrix;
    private final Set<Node> nodes;

    public NetworkGraph(WeightedGraph<Node, Delay> graph) {
        this.adjMatrix = graph;
        this.nodes = graph.keySet();
    }

    public WeightedGraph<Node, Delay> getAdjMatrix() {
        return adjMatrix;
    }

    public Set<Node> getNodes() {
        return nodes;
    }
}