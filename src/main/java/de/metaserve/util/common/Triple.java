package de.metaserve.util.common;

public record Triple<T1, T2, T3>(T1 first, T2 second, T3 third) {
    public static <T1, T2, T3> Triple<T1, T2, T3> of(T1 a, T2 b, T3 c) {
        return new Triple<>(a, b, c);
    }
}
