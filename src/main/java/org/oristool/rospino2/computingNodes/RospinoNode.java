package org.oristool.rospino2.computingNodes;

public class RospinoNode extends AbstractNode {

    public RospinoNode(int id, double totalCores) {
        super(id, totalCores, totalCores);
    }

    public boolean canHost(Integer demand) {
        return this.getAvailableCores() >= demand;
    }

    public void allocate(Integer demand) {
        if (!canHost(demand))
            this.setAvailableCores(this.getAvailableCores() - demand);
    }
}