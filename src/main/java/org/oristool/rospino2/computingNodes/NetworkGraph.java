package org.oristool.rospino2.computingNodes;

import org.oristool.eulero.modeling.stochastictime.StochasticTime;
import org.oristool.eulero.modeling.stochastictime.UniformTime;

import javax.swing.*;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

public class NetworkGraph {
    private final StronglyConnectedWeightedGraph<AbstractNode, StochasticTime> adjMatrix;
    private final Set<AbstractNode> rospinoNodes;

    public NetworkGraph(StronglyConnectedWeightedGraph<AbstractNode, StochasticTime> graph) {
        this.adjMatrix = graph;
        this.rospinoNodes = graph.keySet();
    }

    public StronglyConnectedWeightedGraph<AbstractNode, StochasticTime> getAdjMatrix() {
        return adjMatrix;
    }

    public Set<AbstractNode> getNodes() {
        return rospinoNodes;
    }

    public StochasticTime getDelayBetweenNodes(AbstractNode from, AbstractNode to) {
        if (!rospinoNodes.contains(from) || !rospinoNodes.contains(to)) {
            throw new IllegalArgumentException("Il grafo non contiene nodi");
        }
        Optional<StochasticTime> weight = adjMatrix.getWeight(from, to);
        return weight.orElseGet(()->new UniformTime(BigDecimal.valueOf(Double.MAX_VALUE), BigDecimal.valueOf(Double.MAX_VALUE)));

    }
}