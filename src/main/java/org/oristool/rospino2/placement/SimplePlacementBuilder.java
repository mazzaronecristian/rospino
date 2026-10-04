package org.oristool.rospino2.placement;

import org.oristool.eulero.modeling.Activity;
import org.oristool.eulero.modeling.stochastictime.StochasticTime;
import org.oristool.rospino2.computingNodes.AbstractNode;
import org.oristool.rospino2.computingNodes.NetworkGraph;

import java.math.BigDecimal;
import java.util.*;


public class SimplePlacementBuilder extends PlacementBuilder{
    @Override
    public Placement build() {
        Map<Activity, AbstractNode> assignments = new HashMap<>();
        List<Activity> sortedActivities = topologicalSort(this.getWorkflow().activities());
        NetworkGraph networkGraph = this.getNetworkGraph();
        for (Activity activity : sortedActivities) {
            AbstractNode best = null;
            BigDecimal bestCost = BigDecimal.valueOf(Double.MAX_VALUE);
            Set<AbstractNode> rospinoNodes = networkGraph.getNodes();
            Comparator<AbstractNode> byAvailableCores = Comparator.comparingDouble(AbstractNode::getAvailableCores);
            for (AbstractNode candidate : rospinoNodes.stream().sorted(byAvailableCores).toList()) {
                if (!candidate.canHost(activity)) continue;
                BigDecimal cost = delayCost(activity, assignments, candidate);
                if (cost.compareTo(bestCost) < 0) {
                    bestCost = cost;
                    best = candidate;
                }
            }

            if (best == null)
                throw new IllegalStateException(
                        "Placement impossibile per il servizio: " + activity.toString());

            best.allocate(activity);
            assignments.put(activity, best);
        }

        return new Placement(assignments, this.getNetworkGraph());
    }

    private BigDecimal delayCost(Activity activity, Map<Activity, AbstractNode> assignments, AbstractNode candidate) {
        List<Activity> predecessors = activity.pre();
        BigDecimal cost = BigDecimal.valueOf(0.0);

        for (Activity predecessor : predecessors) {
            AbstractNode predNode = assignments.get(predecessor);
            if (predNode == null) continue;
            StochasticTime delayBetweenNodes = this.getNetworkGraph().getDelayBetweenNodes(predNode, candidate);
            cost = cost.add(delayBetweenNodes.getLFT());
        }
        return cost;
    }

    private List<Activity> topologicalSort(List<Activity> activities) {

        Map<Activity, Integer> inDegree = new HashMap<>();
//?        for (Activity activity : activities) inDegree.put(activity, 0);
        for (Activity activity : activities) {
            int numOfPredecessors = activity.pre().size();
            inDegree.put(activity, numOfPredecessors);
        }

        Map<Activity, List<Activity>> successorsPerService = new HashMap<>();
        for (Activity service : activities) {
            for (Activity predecessor : service.pre()) {
                successorsPerService.computeIfAbsent(predecessor, _ -> new ArrayList<>()).add(service);
            }

        }

        Queue<Activity> queue = new ArrayDeque<>();
        for (Map.Entry<Activity, Integer> e : inDegree.entrySet())
            if (e.getValue() == 0) queue.add(e.getKey());

        List<Activity> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            Activity activity = queue.poll();
            result.add(activity);
            for (Activity successor : successorsPerService.getOrDefault(activity, List.of())) {
                int deg = inDegree.merge(successor, -1, Integer::sum);
                if (deg == 0) queue.add(successor);
            }
        }

        if (result.size() != activities.size())
            throw new IllegalArgumentException("Il DAG contiene un ciclo.");
        return result;
    }
}