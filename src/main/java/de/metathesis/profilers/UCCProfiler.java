package de.metathesis.profilers;


import de.metathesis.structures.AttributeList;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class UCCProfiler extends AbstractProfiler<AttributeList[]> {

    public UCCProfiler(Executor executor) {
        super(executor);
    }

    @Override
    public CompletableFuture<AttributeList[]> runAsync(int level) {
        return CompletableFuture.supplyAsync(
                () -> profile(level),
                executor
        );
    }

    @Override
    public AttributeList[] profile(int number) {
        this.preprocessor.loadRelation("R1");
        for (int i = 1; i <= 5; i++) {
            System.out.println("Profiling UCC:" + number + " = " + i);

            try {
                // Pause the execution for 1 second
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return new AttributeList[0];
    }

    @Override
    public String name() {
        return "UCCProfiler";
    }
}