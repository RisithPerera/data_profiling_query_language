package de.metaserve.model.dpal;

import de.metaserve.model.constraints.Pair;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

@Getter @Setter
public class Quadruple<T1, T2, T3, T4> {
    private T1 first;
    private T2 second;
    private T3 third;
    private T4 fourth;

    public Quadruple(T1 first, T2 second, T3 third, T4 fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quadruple<?, ?, ?, ?> quad = (Quadruple<?, ?, ?, ?>) o;
        return Objects.equals(first, quad.first)
                && Objects.equals(second, quad.second)
                && Objects.equals(third, quad.third)
                && Objects.equals(fourth, quad.fourth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, third, fourth);
    }
}
