package org.oristool.rospino2.computingNodes;

import org.oristool.eulero.modeling.Activity;

public abstract class AbstractNode {
    private int id;
    private double totalCores;
    private double availableCores;

    public int getId() {
        return id;
    }

    public AbstractNode setId(int id) {
        this.id = id;
        return this;
    }

    public double getTotalCores() {
        return totalCores;
    }

    public AbstractNode setTotalCores(double totalCores) {
        this.totalCores = totalCores;
        return this;
    }

    public double getAvailableCores() {
        return availableCores;
    }

    public AbstractNode setAvailableCores(double availableCores) {
        this.availableCores = availableCores;
        return this;
    }

    public AbstractNode(int id, double totalCores, double availableCores) {
        this.id = id;
        this.totalCores = totalCores;
        this.availableCores = availableCores;
    }


    public abstract boolean canHost(Activity a);

    public abstract void allocate(Activity a);

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof RospinoNode other)){
            return false;
        }
        return other.getId() == this.id;
    }

    @Override
    public int hashCode() {
        return this.id;
    }
}