package org.oristool.rospino.computingNodes;

public class Node {
    private int capacity;
    private int id;

    public Node(int capacity, int id) {
        this.capacity = capacity;
        this.id = id;
    }

    public int getCapacity() {
        return capacity;
    }

    public Node setCapacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    public int getId() {
        return id;
    }

    public Node setId(int id) {
        this.id = id;
        return this;
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