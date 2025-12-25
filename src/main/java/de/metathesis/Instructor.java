package de.metathesis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Instructor implements ResultListener, AutoCloseable {
    private final Preprocessor preprocessor;
    private final ExecutorService profilerExecutor;
    private final ConcurrentMap<String, int[][]> results = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(0);

    public Instructor(int parallelism) {
        this.preprocessor = new Preprocessor();
        this.profilerExecutor = (parallelism <= 1)
                ? Executors.newSingleThreadExecutor()
                : Executors.newFixedThreadPool(parallelism);
    }

    // Submit a profiler instance for execution (parallel or serial depends on instructor's executor)
    public Future<?> submitProfiler(Profiler profiler, String profilerId, int[][] input) {
        // If profiler is an AbstractProfiler (we expect this), cast and create runnable
        if (profiler instanceof AbstractProfiler) {
            AbstractProfiler ap = (AbstractProfiler) profiler;
            // ap already contains provider/listener/input in constructor form; if not, adapt here.
            return profilerExecutor.submit(ap);
        } else {
            // Wrap in runnable for generic Profiler implementations
            return profilerExecutor.submit(() -> {
                int[][] result = profiler.profile(input);
                onResult(profilerId, result);
            });
        }
    }

    // Convenience factory to create standard profilers wired to this instructor/preprocessor
    public FDProfiler createFDProfiler(String id, int[][] input) {
        return new FDProfiler(id, preprocessor, this, input);
    }

    public INDProfiler createINDProfiler(String id, int[][] input) {
        return new INDProfiler(id, preprocessor, this, input);
    }

    public UCCProfiler createUCCProfiler(String id, int[][] input) {
        return new UCCProfiler(id, preprocessor, this, input);
    }

    // Blocking call: runs profilers in parallel and waits for all to finish
    public Map<String,int[][]> runParallelAndWait(List<AbstractProfiler> profilers) {
        int n = profilers.size();
        CountDownLatch latch = new CountDownLatch(n);

        for (AbstractProfiler p : profilers) {
            profilerExecutor.submit(() -> {
                try {
                    p.run();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Execution interrupted", e);
        }

        return new HashMap<>(results);
    }

    // Serial run: run profilers one-by-one on the instructor's single-thread executor
    public Map<String,int[][]> runSerialAndWait(List<AbstractProfiler> profilers) {
        for (AbstractProfiler p : profilers) {
            p.run();
        }
        return new HashMap<>(results);
    }

    // Profiler callbacks publish results here
    @Override
    public void onResult(String profilerId, int[][] result) {
        results.put(profilerId, result);
        counter.incrementAndGet();
    }

    public Map<String,int[][]> getCollectedResults() {
        return new HashMap<>(results);
    }

    @Override
    public void close() {
        profilerExecutor.shutdownNow();
        preprocessor.close();
    }
}