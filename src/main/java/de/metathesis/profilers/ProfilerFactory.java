package de.metathesis.profilers;

import de.metaserve.executor.min.graph.edge.Edge;
import de.metaserve.executor.min.graph.edge.FDEdge;
import de.metaserve.executor.min.graph.edge.INDEdge;
import de.metaserve.executor.min.graph.edge.UCCEdge;
import de.metathesis.structures.requests.FDRequest;
import de.metathesis.structures.requests.INDRequest;
import de.metathesis.structures.requests.SearchSpace;
import de.metathesis.structures.requests.UCCRequest;
import de.metathesis.structures.results.Result;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

@Getter
public final class ProfilerFactory {
    private final Executor executor;
    private final UCCProfiler uccProfiler;
    private final INDProfiler indProfiler;
    private final FDProfiler fdProfiler;

    public ProfilerFactory(ExecutorService executor) {
        this.executor = executor;
        this.uccProfiler = new UCCProfiler(executor);
        this.indProfiler = new INDProfiler(executor);
        this.fdProfiler  = new FDProfiler(executor);
    }

    public CompletableFuture<? extends Result<?>> run(Edge edge, SearchSpace lhs, SearchSpace rhs) {
        if (edge instanceof UCCEdge) {
            return this.uccProfiler.runAsync(new UCCRequest(lhs));
        }

        if (edge instanceof INDEdge) {
            return this.indProfiler.runAsync(new INDRequest(lhs, rhs));
        }

        if (edge instanceof FDEdge) {
            return this.fdProfiler.runAsync(new FDRequest(lhs, rhs));
        }

        throw new IllegalArgumentException("Unknown Dependency: " + edge);
    }
}

