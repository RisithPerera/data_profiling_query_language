package de.metathesis.structures.results;

import de.metathesis.structures.AttributeBitSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class FDResult implements Iterable<FDResult.FD>{

    private final ObjectArrayList<AttributeBitSet> lhs = new ObjectArrayList<>();
    private final ObjectArrayList<AttributeBitSet> rhs = new ObjectArrayList<>();

    public void add(AttributeBitSet lhsBitSet, AttributeBitSet rhsBitSet) {
        assert (lhsBitSet.getRelationIndex() == rhsBitSet.getRelationIndex());

        lhs.add(lhsBitSet);
        rhs.add(rhsBitSet);
    }

    public int size() {
        return lhs.size();
    }

    public boolean isEmpty() {
        return lhs.isEmpty();
    }

    public FD get(int index) {
        return new FD(lhs.get(index), rhs.get(index));
    }

    public ObjectArrayList<AttributeBitSet> asLhsList() {
        return this.lhs;
    }

    public ObjectArrayList<AttributeBitSet> asRhsList() {
        return this.rhs;
    }

    /* --- Separate Iteration --- */
    public Iterable<AttributeBitSet> lhs() {
        return lhs;
    }

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

    /* --- Internal IND Object --- */
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
