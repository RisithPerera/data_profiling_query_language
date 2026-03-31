package de.metathesis.structures;

import de.metaserve.executor.min.graph.edge.Edge;
import de.metathesis.ResultFormatter;
import de.metathesis.Utility;
import de.metathesis.profilers.ProfilerFactory;
import de.metathesis.structures.requests.SearchSpace;
import de.metathesis.structures.results.FDResult;
import de.metathesis.structures.results.INDResult;
import de.metathesis.structures.results.Result;
import de.metathesis.structures.results.UCCResult;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Getter
public final class ExecutionNode {
    private final Edge edge;
    private final int level;
    private final ProfilerFactory factory;

    private final Map<String, int[]> relationMap = new HashMap<>();
    private final Map<String, ExecutionNode> parents = new HashMap<>();
    private final List<ExecutionNode> children = new ArrayList<>(); //To Track Leaf Nodes

    private CompletableFuture<? extends Result<?>> future;
    private Result<?> results;

    public ExecutionNode(Edge edge, int level, ProfilerFactory factory) {
        this.edge = edge;
        this.level = level;
        this.factory = factory;
    }

    public void execute() {
        if (Objects.nonNull(this.future)) {
            return; // prevent double execution
        }

        CompletableFuture<?>[] parents = this.parents.values().stream()
                    .map(ExecutionNode::getFuture)
                    .toArray(CompletableFuture[]::new);

        this.future = CompletableFuture.allOf(parents) //Waiting for all parents to complete
                .thenCompose(v -> {
                    SearchSpace lhsSearchSpace = initializeSearchSpaceFor(this.edge.leftName);
                    SearchSpace rhsSearchSpace = initializeSearchSpaceFor(this.edge.rightName);

                    Utility.printLog(String.format("S: %s", this), this.factory.getExecutor());
                    return this.factory.run(this.edge, lhsSearchSpace, rhsSearchSpace);
                });

        //Once this node is completed crop it's parent lhs and rhs results.
        this.future.thenAccept(out -> {
            // Result handling belongs here
            Utility.printLog(String.format("F: %s", this), this.factory.getExecutor());

            results = out;
            //showResults(); //Testing Purposes
            cropParentResults();
        });


    }

    private SearchSpace initializeSearchSpaceFor(String variable){
        final boolean isLocked = this.parents.containsKey(variable);

        if (isLocked) {
            final ExecutionNode connectedNode = this.parents.get(variable);
            Result<?> nodeResults = connectedNode.getFuture().join(); //Safe join

            //Check the variable is matched with parent lhs variable.
            boolean isLockedWithLhs =  variable.equals(connectedNode.getEdge().leftName);

            //If isLockedWithLhs True then take lhs attributeSet list otherwise get rhs attributeSet list
            ObjectOpenHashSet<AttributeBitSet> lhsLockedSpace = isLockedWithLhs ? nodeResults.asLhsSet() : nodeResults.asRhsSet();
            return new SearchSpace.Locked(lhsLockedSpace);
        }else{
            return new SearchSpace.CC(this.relationMap.get(variable), this.level);
        }
    }

    private void showResults(){
        List<de.metanome.algorithm_integration.results.Result> resultList = new ArrayList<>();

        for(Object x:  this.getResults()) {
            if(x instanceof UCCResult.UCC ucc){
                resultList.add(ResultFormatter.getInstance().formatUCC(ucc));
            }else if(x instanceof INDResult.IND ind){
                resultList.add(ResultFormatter.getInstance().formatIND(ind));
            }else if(x instanceof FDResult.FD fd){
                resultList.add(ResultFormatter.getInstance().formatFD(fd));
            }
        }
        System.out.println(resultList);
    }

    private void cropParentResults(){
        for (ExecutionNode parent : this.parents.values()) {
            if(parent.getEdge().equals(this.edge)){
                return;
            }

            System.out.println("Cropping Results Parent: " + parent + " By Child: " + this);
            String parentLeftName = parent.getEdge().leftName;
            String parentRightName = parent.getEdge().rightName;

            String leftName = this.edge.leftName;
            String rightName = this.edge.rightName;

            if(parentLeftName.equals(leftName)) {
                parent.getResults().cropByLhsSet(this.getResults().asLhsSet());
            }

            if(parentRightName.equals(leftName)) {
                parent.getResults().cropByRhsSet(this.getResults().asLhsSet());
            }

            if(parentLeftName.equals(rightName)) {
                parent.getResults().cropByLhsSet(this.getResults().asRhsSet());
            }

            if(parentRightName.equals(rightName)) {
                parent.getResults().cropByRhsSet(this.getResults().asRhsSet());
            }
        }
    }

    public String getPreviousNodeId() {
        return this.edge + "_" + (this.level - 1);
    }

    @Override
    public String toString() {
        return this.edge + "_" + this.level;
    }
}

