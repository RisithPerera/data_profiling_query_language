package de.metathesis;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/*
This class is for regular helper method. Later will be refactored the code accordingly.
 */
public class Utility {
    public static int max(Collection<int[]> arr) {
        return arr.stream()
                .mapToInt(Utility::max)
                .max()
                .orElse(0);
    }

    public static int max(int[] arr) {
        int m = 0;
        for (int s : arr) m = Math.max(m, s);
        return m;
    }

    public static int[] intersect(int[] arr1, int[] arr2) {
        Set<Integer> setA = new HashSet<>();
        for (int x : arr1){
            setA.add(x);
        }

        Set<Integer> common = new HashSet<>();
        for (int x : arr2) {
            if (setA.contains(x)) {
                common.add(x);
            }
        }

        int[] result = new int[common.size()];
        int i = 0;
        for (int x : common) {
            result[i++] = x;
        }

        return result;
    }

    // Helper Methods
    public static int binomial(int n, int k) {
        if (k < 0 || k > n) return 0;
        if (k == 0 || k == n) return 1;
        long res = 1;
        for (int i = 1; i <= k; i++) {
            res = res * (n - i + 1) / i;
        }
        return (int) res;
    }

    public static int[] filterByLevel(int[] relations, int[] sizes, int level) {

        List<Integer> filtered = new ArrayList<>();
        for (int i = 0; i < relations.length; i++) {
            if (sizes[i] >= level) {
                filtered.add(relations[i]);
            }
        }
        return filtered.stream().mapToInt(Integer::intValue).toArray();
    }

    public static void printLog(String tag, Executor pool){
        // 1. Define your executor
        ThreadPoolExecutor threadPool = (ThreadPoolExecutor) pool;

        // 2. Later in your code, or in a background "Monitor" thread:
        System.out.printf(
                "[%s] [%d/%d] Active: %d, Completed: %d, Queue: %d%n",
                tag,
                threadPool.getPoolSize(),
                threadPool.getMaximumPoolSize(),
                threadPool.getActiveCount(),
                threadPool.getCompletedTaskCount(),
                threadPool.getQueue().size()
        );
    }
}
