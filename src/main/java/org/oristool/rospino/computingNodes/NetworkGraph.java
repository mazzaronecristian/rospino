package org.oristool.rospino.computingNodes;

import java.util.Optional;
import java.util.Set;

public class NetworkGraph {
    private final StronglyConnectedWeightedGraph<Node, Delay> adjMatrix;
    private final Set<Node> nodes;

    public NetworkGraph(StronglyConnectedWeightedGraph<Node, Delay> graph) {
        this.adjMatrix = graph;
        this.nodes = graph.keySet();
    }

    public StronglyConnectedWeightedGraph<Node, Delay> getAdjMatrix() {
        return adjMatrix;
    }

    public Set<Node> getNodes() {
        return nodes;
    }

    public Delay getDelayBetweenNodes(Node from, Node to) {
        if (!nodes.contains(from) || !nodes.contains(to)) {
            throw new IllegalArgumentException("Il grafo non contiene nodi");
        }
        Optional<Delay> weight = adjMatrix.getWeight(from, to);
        return weight.orElseGet(Delay::getInfiniteDelay);

    }
}