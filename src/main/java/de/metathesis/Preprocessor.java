package de.metathesis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class Preprocessor implements AutoCloseable {
    // Single thread executor to guarantee one-at-a-time execution of preprocess requests.
    private final ExecutorService singleThread = Executors.newSingleThreadExecutor();

    // Optional cache to avoid repeated preprocessing for identical input (simple hash).
    private final ConcurrentMap<Integer, List<List<String>>> cache = new ConcurrentHashMap<>();

    public List<List<String>> get(int[][] raw) {
        int key = Arrays.deepHashCode(raw);
        List<List<String>> cached = cache.get(key);
        if (cached != null) return cached;

        // Submit preprocessing task to single-thread executor and wait for result.
        Future<List<List<String>>> future = singleThread.submit(() -> doPreprocess(raw));
        try {
            List<List<String>> result = future.get(); // blocks here but executor enforces seriality
            cache.putIfAbsent(key, result);
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Preprocessing interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Preprocessing failed", e.getCause());
        }
    }

    private List<List<String>> doPreprocess(int[][] raw) {
        // Skeleton: convert int[][] columns into List<List<String>> (each column -> List<String>)
        // No heavy logic; user will implement actual conversion.
        if (raw == null || raw.length == 0) return Collections.emptyList();
        int rows = raw.length;
        int cols = raw[0].length;
        List<List<String>> columns = new ArrayList<>(cols);
        for (int c = 0; c < cols; c++) {
            List<String> col = new ArrayList<>(rows);
            for (int r = 0; r < rows; r++) {
                col.add(String.valueOf(raw[r][c]));
            }
            columns.add(col);
        }
        return columns;
    }

    @Override
    public void close() {
        singleThread.shutdownNow();
    }
}

