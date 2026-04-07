package de.metathesis;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/*
This class is for regular helper method. Later will be refactored the code accordingly.
 */
public class Utility {
    public static long compositeKey(int a, int b) {
        return ((long) a << 32) | (b & 0xffffffffL);
    }

    public static long compositeKey(int a, int b, int c) {
        return ((long) a << 42) | ((long) b << 21) | (long) c;
    }

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

    // Helper Methods
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

    // Convert BigInteger mask to BitSet
    public static BitSet toBitSet(BigInteger mask, int cols) {
        BitSet bs = new BitSet(cols);
        for (int i = 0; i < cols; i++) {
            if (mask.testBit(i)) bs.set(i);
        }
        return bs;
    }

    public static boolean addMaximal(List<BitSet> sets, BitSet newSet) {
        for (BitSet existing : sets) {
            if (isSubset(newSet, existing)) {
                return false;
            }
        }
        sets.removeIf(existing -> isSubset(existing, newSet));
        sets.add((BitSet) newSet.clone());
        return true;
    }

    public static void addMinimal(List<BitSet> sets, BitSet newSet) {
        for (BitSet existing : sets) {
            if (isSubset(existing, newSet)) {
                return;
            }
        }
        sets.removeIf(existing -> isSubset(newSet, existing));
        sets.add((BitSet) newSet.clone());
    }

//    public static void addMinimalWithSplit(List<BitSet> sets, BitSet newSet) {
//        for (BitSet existing : sets) {
//            if (isSubset(existing, newSet)) {
//                return;
//            }
//        }
//
//        List<BitSet> remainders = new ArrayList<>();
//
//        sets.removeIf(existing -> {
//            if (isSubset(newSet, existing)) {
//                BitSet remainder = (BitSet) existing.clone();
//                remainder.andNot(newSet); // strip out newSet bits gives (C,D)
//                if (!remainder.isEmpty()) {
//                    remainders.add(remainder);
//                }
//                return true; // remove the original superset
//            }
//            return false;
//        });
//
//        sets.add((BitSet) newSet.clone());
//        sets.addAll(remainders);
//    }

    public static void addMinimalWithSplit(List<BitSet> sets, BitSet parent, BitSet confirmed) {
        // Remove the parent candidate
        sets.remove(parent);

        // Add confirmed minimal
        addMinimal(sets, confirmed);

        // Add remaining bits as separate candidate
        BitSet remaining = (BitSet) parent.clone();
        remaining.andNot(confirmed);

        if (remaining.cardinality() > 0) {
            addMinimal(sets, remaining);
        }
    }

    public static boolean isSubset(BitSet a, BitSet b) {
        BitSet temp = (BitSet) a.clone();
        temp.and(b);
        return temp.equals(a);
    }

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
                "[%-15s] [%2d/%d] Active: %d, Completed: %d, Queue: %d%n",
                tag,
                threadPool.getPoolSize(),
                threadPool.getMaximumPoolSize(),
                threadPool.getActiveCount(),
                threadPool.getCompletedTaskCount(),
                threadPool.getQueue().size()
        );
    }
}
