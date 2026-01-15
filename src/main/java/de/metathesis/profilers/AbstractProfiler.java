package de.metathesis.profilers;

import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.Preprocessor;
import de.metathesis.structures.requests.Request;
import de.metathesis.structures.results.Result;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public abstract class AbstractProfiler<In extends Request, Out extends Result<?>> implements Profiler<In, Out> {
    @Getter
    protected final Preprocessor preprocessor;

    @Getter
    protected final Executor executor;

    public AbstractProfiler(Executor executor) {
        this.executor = executor;
        this.preprocessor = Preprocessor.getInstance();
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
