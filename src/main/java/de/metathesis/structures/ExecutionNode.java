package de.metathesis.structures;

import de.metaserve.executor.min.graph.edge.Edge;
import de.metathesis.Utility;
import de.metathesis.profilers.AbstractProfiler;
import de.metathesis.structures.requests.Request;
import de.metathesis.structures.results.FDResult;
import de.metathesis.structures.results.INDResult;
import de.metathesis.structures.results.Result;
import de.metathesis.structures.results.UCCResult;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Getter
public final class ExecutionNode<In extends Request, Out extends Result<?>> {
    private final Edge edge;
    private final int level;
    private final AbstractProfiler<In, Out> profiler;
    private final Function<Map<ExecutionNode<?, ?>, Result<?>>, In> inputBuilder;

    private final List<ExecutionNode<?, ?>> parents = new ArrayList<>();
    private final List<ExecutionNode<?, ?>> children = new ArrayList<>(); //To Track Leaf Nodes

    private CompletableFuture<Out> future;
    private Out results;

    public ExecutionNode(Edge edge,
                         int level,
                         AbstractProfiler<In, Out> profiler,
                         Function<Map<ExecutionNode<?, ?>, Result<?>>, In> inputBuilder) {
        this.edge = edge;
        this.level = level;
        this.profiler = profiler;
        this.inputBuilder = inputBuilder;
    }

    public void execute() {
        if (Objects.nonNull(this.future)) {
            return; // prevent double execution
        }

        CompletableFuture<?>[] parents = this.parents.stream()
                        .map(ExecutionNode::getFuture)
                        .toArray(CompletableFuture[]::new);

        this.future = CompletableFuture.allOf(parents) //Waiting for all parents to complete
                        .thenCompose(v -> {
                            Map<ExecutionNode<?, ?>, Result<?>> depResults = new HashMap<>();
                            for (ExecutionNode<?, ?> p : this.parents) {
                                depResults.put(p, p.getFuture().join()); // Collecting parent results (safe join)
                            }

                            In input = inputBuilder.apply(depResults);

                            Utility.printLog(String.format("S: %s", this), this.profiler.getExecutor());
                            return profiler.runAsync(input);
                        });

        //Once this node is completed crop it's parent lhs and rhs results.
        this.future.thenAccept(out -> {
            // Result handling belongs here
            Utility.printLog(String.format("F: %s", this), this.profiler.getExecutor());

            results = out;
            showResults(); //Testing Purposes
            cropParentResults();
        });
    }

    private void showResults(){
        List<de.metanome.algorithm_integration.results.Result> resultList = new ArrayList<>();

        for(Object x:  this.getResults()) {
            if(x instanceof UCCResult.UCC ucc){
                resultList.add(this.profiler.getPreprocessor().formatUCC(ucc));
            }else if(x instanceof INDResult.IND ind){
                resultList.add(this.profiler.getPreprocessor().formatIND(ind));
            }else if(x instanceof FDResult.FD fd){
                resultList.add(this.profiler.getPreprocessor().formatFD(fd));
            }
        }
        System.out.println(resultList);
    }

    private void cropParentResults(){
        System.out.println("Cropping parent results: "+ this);
        for (ExecutionNode<?, ?> parent : this.parents) {
            if(parent.getEdge().equals(this.edge)){
                return;
            }

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

