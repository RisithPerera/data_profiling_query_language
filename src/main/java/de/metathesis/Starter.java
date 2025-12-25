package de.metathesis;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Starter {

    public static void main(String[] args) {
        int[][] dataset = new int[][]{
                {1, 2, 3},
                {4, 2, 3},
                {1, 5, 6}
        };

        Instructor instructor = new Instructor(Runtime.getRuntime().availableProcessors());
        try {
            // create three profiler instances wired to same preprocessor + instructor
            FDProfiler fd = instructor.createFDProfiler("FD-1", dataset);
            INDProfiler ind = instructor.createINDProfiler("IND-1", dataset);
            UCCProfiler ucc = instructor.createUCCProfiler("UCC-1", dataset);

            List<AbstractProfiler> profilers = Arrays.asList(fd, ind, ucc);

            // run them in parallel and wait
            Map<String, int[][]> results = instructor.runParallelAndWait(profilers);
            // results map contains entries keyed by profilerId (values are int[][] placeholders)
        } finally {
            instructor.close();
        }
    }
}