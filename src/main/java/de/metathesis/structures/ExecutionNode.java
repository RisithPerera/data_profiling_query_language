package de.metathesis.structures;

import de.metathesis.profilers.AbstractProfiler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ExecutionNode<I, O> {

    public final String id;                 // e.g. "UCC(Y)-L2"
    public final AbstractProfiler<I, O> profiler;
    public final I input;

    public final List<ExecutionNode<?, ?>> dependsOn = new ArrayList<>();
    public final List<ExecutionNode<?, ?>> dependents = new ArrayList<>();

    CompletableFuture<O> future;

    public ExecutionNode(String id, AbstractProfiler<I, O> profiler, I input) {
        this.id = id;
        this.profiler = profiler;
        this.input = input;
    }
}

