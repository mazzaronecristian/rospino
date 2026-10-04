package org.oristool.rospino2.placement;

import org.oristool.eulero.modeling.Activity;
import org.oristool.rospino2.computingNodes.AbstractNode;

public class Rospino2Node extends AbstractNode {
    public Rospino2Node(int id, double totalCores) {
        super(id, totalCores, totalCores);
    }

    @Override
    public void allocate(Activity a) {
        if (!canHost(a)) {
            throw new IllegalArgumentException("Implementami");
        }
    }

    @Override
    public boolean canHost(Activity a) {
        return true;
    }
}