package de.metathesis.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

public class MemoryUtils {
    private static MemoryUsage getUsage() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    }

    // getMax() = Max memory as specified by the jvm -Xmx flag (-1 if not specified)
    // getCommitted() = Currently allocated memory to the JVM by the OS (can be smaller than max!)
    // getUsed() = Currently used memory by the application

    public static long bytesFree() {
        final long max = getUsage().getMax();
        final long committed = getUsage().getCommitted();
        final long used = getUsage().getUsed();

        return (max < 0) ? committed - used : max - used;
    }

    public static long bytesAllocated() {
        final long max = getUsage().getMax();
        final long committed = getUsage().getCommitted();

        return (max < 0) ? committed : max;
    }

    public static double systemMemoryUsage() {
        final long max = getUsage().getMax();
        final long committed = getUsage().getCommitted();
        final long used = getUsage().getUsed();

        return (max < 0) ? used * 1.0 / committed : used * 1.0 / max;
    }
}
