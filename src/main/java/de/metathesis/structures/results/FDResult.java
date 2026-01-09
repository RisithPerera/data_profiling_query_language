package de.metathesis.structures.results;

import de.metathesis.structures.AttributeBitSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class FDResult implements Result<FDResult.FD>{

    private final ObjectArrayList<AttributeBitSet> lhs = new ObjectArrayList<>();
    private final ObjectArrayList<AttributeBitSet> rhs = new ObjectArrayList<>();

    public void add(AttributeBitSet lhsBitSet, AttributeBitSet rhsBitSet) {
        assert (lhsBitSet.getRelationIndex() == rhsBitSet.getRelationIndex());

        if(lhs.contains(lhsBitSet) && rhs.contains(rhsBitSet)) return;

        lhs.add(lhsBitSet);
        rhs.add(rhsBitSet);
    }

    @Override
    public int size() {
        return lhs.size();
    }

    @Override
    public boolean isEmpty() {
        return lhs.isEmpty();
    }

    @Override
    public FD get(int index) {
        return new FD(lhs.get(index), rhs.get(index));
    }

    @Override
    public ObjectOpenHashSet<AttributeBitSet> asLhsSet() {
        return new ObjectOpenHashSet<>(lhs);
    }

    @Override
    public ObjectOpenHashSet<AttributeBitSet> asRhsSet() {
        return new ObjectOpenHashSet<>(rhs);
    }

    /* --- Separate Iteration --- */
    @Override
    public Iterable<AttributeBitSet> lhs() {
        return lhs;
    }

    @Override
    public Iterable<AttributeBitSet> rhs() {
        return rhs;
    }

    @Override
    public Iterator<FD> iterator() {
        return new Iterator<>() {
            private int idx = 0;

            @Override
            public boolean hasNext() {
                return idx < lhs.size();
            }

            @Override
            public FD next() {
                if (!hasNext()) throw new NoSuchElementException();
                return new FD(lhs.get(idx), rhs.get(idx++));
            }
        };
    }

    /* --- Internal FD Object --- */
    public static final class FD {
        public final AttributeBitSet lhs;
        public final AttributeBitSet rhs;

        private FD(AttributeBitSet lhs, AttributeBitSet rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public String toString() {
            return lhs.toString() + " -> " + rhs.toString();
        }
    }
}
