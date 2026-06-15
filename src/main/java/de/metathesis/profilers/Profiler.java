package de.metathesis.profilers;


import de.metaserve.util.exceptions.InputIterationException;

import java.util.concurrent.CompletableFuture;

interface Profiler<In, Out> {

    CompletableFuture<Out> runAsync(In input);

    Out profile(In input) throws InputIterationException;
}