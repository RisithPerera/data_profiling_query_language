package de.metathesis.profilers;

import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.ProfilingContext;
import de.metathesis.profilers.requests.Request;
import de.metathesis.profilers.results.Result;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public abstract class AbstractProfiler<In extends Request, Out extends Result<?>> implements Profiler<In, Out> {
    @Getter
    protected final ProfilingContext profilingContext;

    @Getter
    protected final ExecutorService executor;

    public AbstractProfiler(ExecutorService executor) {
        this.executor = executor;
        this.profilingContext = ProfilingContext.getInstance();
    }

    @Override
    public CompletableFuture<Out> runAsync(In input) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return profile(input);
                    } catch (InputIterationException e) {
                        throw new RuntimeException(e);
                    }
                },
                executor
        );
    }
}
