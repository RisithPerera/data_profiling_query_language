package de.metathesis.utils;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
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

    // Helper Methods
    public static int[] intersect(int[] arr1, int[] arr2) {
        BitSet bs = new BitSet();
        for (int v : arr1) bs.set(v);

        BitSet bs2 = new BitSet();
        for (int v : arr2) bs2.set(v);

        bs.and(bs2); // intersection in one operation
        return bs.stream().toArray();
    }

    public static IntArrayList buildKey(BitSet remainingLhs, int[] compressedRecord) {
        IntArrayList key = new IntArrayList();
        for (int attr = remainingLhs.nextSetBit(0); attr >= 0; attr = remainingLhs.nextSetBit(attr + 1)) {
            int v = compressedRecord[attr];
            if (v == -1) {
                return null;
            }
            key.add(v);
        }
        return key;
    }

    public static int[][] combinations(int[] pool, int k) {
        int n = pool.length;
        int count = 1;
        for (int i = 0; i < k; i++) count = count * (n - i) / (i + 1);

        int[][] result = new int[count][k];
        int[] indices = new int[k];
        for (int i = 0; i < k; i++) indices[i] = i;

        int r = 0;
        while (r < count) {
            for (int i = 0; i < k; i++) {
                result[r][i] = pool[indices[i]];
            }
            r++;

            int pos = k - 1;
            while (pos >= 0 && indices[pos] == n - k + pos) pos--;
            if (pos < 0) break;
            indices[pos]++;
            for (int i = pos + 1; i < k; i++) {
                indices[i] = indices[i - 1] + 1;
            }
        }

        return result;
    }

    public static List<int[]> cartesianProduct(BitSet[] sets) {
        List<int[]> result = new ArrayList<>();
        cartesianProductRecursive(sets, 0, new int[sets.length], result);
        return result;
    }

    private static void cartesianProductRecursive(BitSet[] sets, int pos, int[] current, List<int[]> result) {
        if (pos == sets.length) {
            result.add(current.clone());
            return;
        }
        for (int val = sets[pos].nextSetBit(0); val >= 0; val = sets[pos].nextSetBit(val + 1)) {
            boolean duplicate = false;
            for (int i = 0; i < pos; i++) {
                if (current[i] == val) { duplicate = true; break; }
            }
            if (!duplicate) {
                current[pos] = val;
                cartesianProductRecursive(sets, pos + 1, current, result);
            }
        }
    }

    public static void match(BitSet agree, int[] row1, int[] row2){
        agree.clear();
        for (int col = 0; col < row1.length; col++) {
            if (row1[col] != -1 && row1[col] == row2[col]) {
                agree.set(col);
            }
        }
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

    public static String buildLog(String tag, Executor pool) {
        ThreadPoolExecutor threadPool = (ThreadPoolExecutor) pool;

        return String.format(
                "[%-15s] [%2d/%d] Active: %d, Completed: %d, Queue: %d",
                tag,
                threadPool.getPoolSize(),
                threadPool.getMaximumPoolSize(),
                threadPool.getActiveCount(),
                threadPool.getCompletedTaskCount(),
                threadPool.getQueue().size()
        );
    }

    public static String bitSetToJsonArray(BitSet bs) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            sb.append(i).append(",");
        }
        if (sb.length() > 1) sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }
}