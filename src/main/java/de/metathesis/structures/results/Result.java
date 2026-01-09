package de.metathesis.structures.results;

import de.metathesis.structures.AttributeBitSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public interface Result<T> extends Iterable<T>{
    T get(int index);
    int size();
    boolean isEmpty();

    ObjectOpenHashSet<AttributeBitSet> asLhsSet();
    ObjectOpenHashSet<AttributeBitSet> asRhsSet();

    /* --- Separate Iteration --- */
    Iterable<AttributeBitSet> lhs();
    Iterable<AttributeBitSet> rhs();
}
