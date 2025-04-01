package de.metaserve.util.common;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class Triple<T1, T2, T3> {
    private T1 first;
    private T2 second;
    private T3 third;

    public Triple(T1 first, T2 second, T3 third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Triple<?, ?, ?> quad = (Triple<?, ?, ?>) o;
        return Objects.equals(first, quad.first)
                && Objects.equals(second, quad.second)
                && Objects.equals(third, quad.third);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, third);
    }
}