package org.oristool.rospino2.computingNodes;

import org.oristool.eulero.modeling.Activity;

public class RospinoNode extends AbstractNode {

    public RospinoNode(int id, double totalCores) {
        super(id, totalCores, totalCores);
    }

    public boolean canHost(Activity a) {
        throw new  IllegalArgumentException("da implementare");
//        return this.getAvailableCores() >= a.getDemand();
    }

    public void allocate(Activity a) {
        if (!canHost(a))
            throw new IllegalStateException(
                    "Nodo " + this.getId() + " senza capacità per " + a.toString());
//        this.setAvailableCores(this.getAvailableCores() - a.getDemand());
    }
}