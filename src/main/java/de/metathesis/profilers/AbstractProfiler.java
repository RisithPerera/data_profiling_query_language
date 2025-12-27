package de.metathesis.profilers;


import de.metathesis.Preprocessor;

import java.util.concurrent.Executor;

public abstract class AbstractProfiler<T> implements Profiler<T> {
    protected final Preprocessor preprocessor = Preprocessor.getInstance();
    protected final Executor executor;

    public AbstractProfiler(Executor executor) {
        this.executor = executor;
    }
}
