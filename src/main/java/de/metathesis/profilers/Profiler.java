package de.metathesis.profilers;

import javax.management.AttributeList;
import java.util.concurrent.CompletableFuture;

interface Profiler<T> {

    CompletableFuture<T> runAsync(int level);

    T profile(int number);

    String name();
}