package de.metathesis.structures.results;

import de.metathesis.structures.AttributeBitSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class INDResult implements Iterable<INDResult.IND>{

    private final ObjectArrayList<AttributeBitSet> lhs = new ObjectArrayList<>();
    private final ObjectArrayList<AttributeBitSet> rhs = new ObjectArrayList<>();

    public void add(AttributeBitSet lhsBitSet, AttributeBitSet rhsBitSet) {
        assert (lhsBitSet.size() == rhsBitSet.size());

        lhs.add(lhsBitSet);
        rhs.add(rhsBitSet);
    }

    public int size() {
        return lhs.size();
    }

    public boolean isEmpty() {
        return lhs.isEmpty();
    }

    public IND get(int index) {
        return new IND(lhs.get(index), rhs.get(index));
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
    public Iterator<IND> iterator() {
        return new Iterator<>() {
            private int idx = 0;

            @Override
            public boolean hasNext() {
                return idx < lhs.size();
            }

            @Override
            public IND next() {
                if (!hasNext()) throw new NoSuchElementException();
                return new IND(lhs.get(idx), rhs.get(idx++));
            }
        };
    }

    /* --- Internal IND Object --- */
    public static final class IND {
        public final AttributeBitSet lhs;
        public final AttributeBitSet rhs;

        private IND(AttributeBitSet lhs, AttributeBitSet rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public String toString() {
            return lhs.toString() + " C " + rhs.toString();
        }
    }
}
