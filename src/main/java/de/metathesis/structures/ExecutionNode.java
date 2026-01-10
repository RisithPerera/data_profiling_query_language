package de.metathesis.structures;

import de.metaserve.executor.min.graph.edge.Edge;
import de.metathesis.Utility;
import de.metathesis.profilers.AbstractProfiler;
import de.metathesis.structures.requests.Request;
import de.metathesis.structures.results.Result;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public ExecutionNode(Edge edge,
                         int level,
                         AbstractProfiler<In, Out> profiler,
                         Function<Map<ExecutionNode<?, ?>, Result<?>>, In> inputBuilder) {
        this.edge = edge;
        this.level = level;
        this.profiler = profiler;
        this.inputBuilder = inputBuilder;
    }

    public CompletableFuture<Out> execute() {
        CompletableFuture<?>[] parents = this.parents.stream()
                        .map(ExecutionNode::getFuture)
                        .toArray(CompletableFuture[]::new);

        this.future = CompletableFuture.allOf(parents) //Waiting for all parents to complete
                        .thenCompose(v -> {
                            Map<ExecutionNode<?, ?>, Result<?>> depResults = new HashMap<>();
                            for (ExecutionNode<?, ?> p : this.parents) {
                                depResults.put(p, p.getFuture().join()); //Collecting parent results (safe join)
                            }

                            In input = inputBuilder.apply(depResults);

                            Utility.printLog(String.format("F: %s", this), this.profiler.getExecutor());
                            return profiler.runAsync(input);
                        });

        return future;
    }

    public String getPreviousNodeId() {
        return this.edge + "_" + (this.level - 1);
    }

    @Override
    public String toString() {
        return this.edge + "_" + this.level;
    }
}

