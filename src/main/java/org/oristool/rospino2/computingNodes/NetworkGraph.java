package org.oristool.rospino2.computingNodes;

import org.oristool.eulero.modeling.stochastictime.StochasticTime;
import org.oristool.eulero.modeling.stochastictime.UniformTime;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

public class NetworkGraph {
    private final BidirectionalWeightedGraph<AbstractNode> adjMatrix;
    private final Set<AbstractNode> rospinoNodes;

    public NetworkGraph(BidirectionalWeightedGraph<AbstractNode> graph) {
        this.adjMatrix = graph;
        this.rospinoNodes = graph.nodes();
    }

    public BidirectionalWeightedGraph<AbstractNode> getAdjMatrix() {
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
