package org.oristool.rospino.computingNodes;

import org.oristool.rospino.placement.Service;

public class Node {
    private int id;
    private double totalCores;
    private double availableCores;


    public double getAvailableCores() {
        return availableCores;
    }

    public void setAvailableCores(double availableCores) {
        this.availableCores = availableCores;
    }

    public Node(int id, double totalCores) {
        this.totalCores = totalCores;
        this.availableCores = totalCores;
        this.id = id;
    }


    public double getTotalCores() {
        return totalCores;
    }

    public Node setTotalCores(double totalCores) {
        this.totalCores = totalCores;
        return this;
    }

    public int getId() {
        return id;
    }

    public Node setId(int id) {
        this.id = id;
        return this;
    }


    public boolean canHost(Service a) {
        return availableCores >= a.getDemand();
    }

    public void allocate(Service a) {
        if (!canHost(a))
            throw new IllegalStateException(
                    "Nodo " + id + " senza capacità per " + a.getId());
        availableCores -= a.getDemand();
    }


    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Node other)){
            return false;
        }
        return other.getId() == this.id;
    }

    @Override
    public int hashCode() {
        return this.id;
    }
}