package de.metathesis.profilers;

import de.metanome.algorithm_integration.input.InputIterationException;

import java.util.concurrent.CompletableFuture;

interface Profiler<T> {

    CompletableFuture<T> runAsync(int level);

    T profile(int number) throws InputIterationException;
}