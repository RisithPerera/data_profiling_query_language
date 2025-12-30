package de.metathesis.profilers;

import de.metanome.algorithm_integration.input.InputIterationException;

import java.util.concurrent.CompletableFuture;

interface Profiler<In, Out> {

    CompletableFuture<Out> runAsync(In input);

    Out profile(In input) throws InputIterationException;
}