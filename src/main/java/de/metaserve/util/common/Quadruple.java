package de.metaserve.util.common;

public record Quadruple<T1, T2, T3, T4>(T1 first, T2 second, T3 third, T4 fourth) {

    public static <T1, T2, T3, T4> Quadruple<T1, T2, T3, T4> of(T1 a, T2 b, T3 c, T4 d) {
        return new Quadruple<>(a, b, c, d);
    }
}
