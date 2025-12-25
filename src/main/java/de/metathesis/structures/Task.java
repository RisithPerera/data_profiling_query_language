package de.metathesis.structures;

import de.metaserve.executor.min.graph.Graph;

public class Task {
    String type;
    String[] tables;
    Graph.SetMembership membership;

    public Task(String type, String[] tables, Graph.SetMembership membership) {
        this.type = type;
        this.tables = tables;
        this.membership = membership;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String[] getTables() {
        return tables;
    }

    public void setTables(String[] tables) {
        this.tables = tables;
    }

    public Graph.SetMembership getMembership() {
        return membership;
    }

    public void setMembership(Graph.SetMembership membership) {
        this.membership = membership;
    }
}
