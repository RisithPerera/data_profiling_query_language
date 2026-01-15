package de.metathesis.structures.results;

import de.metathesis.structures.AttributeBitSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class INDResult implements Result<INDResult.IND>{

    private final ObjectArrayList<AttributeBitSet> lhs = new ObjectArrayList<>();
    private final ObjectArrayList<AttributeBitSet> rhs = new ObjectArrayList<>();

    public void add(AttributeBitSet lhsBitSet, AttributeBitSet rhsBitSet) {
        assert (lhsBitSet.size() == rhsBitSet.size());

        int index = lhs.indexOf(lhsBitSet);
        if (index >= 0) {
            if (rhs.get(index).equals(rhsBitSet)) {
                return; // exact pair already exists
            }
        }

        lhs.add(lhsBitSet);
        rhs.add(rhsBitSet);
    }

    @Override
    public void cropByLhsSet(ObjectOpenHashSet<AttributeBitSet> lhsSet) {
        for (int i = lhs.size() - 1; i >= 0; i--) {
            if (!lhsSet.contains(lhs.get(i))) {
                lhs.remove(i);
                rhs.remove(i);
            }
        }
    }


    @Override
    public void cropByRhsSet(ObjectOpenHashSet<AttributeBitSet> rhsSet) {
        for (int i = rhs.size() - 1; i >= 0; i--) {
            if (!rhsSet.contains(rhs.get(i))) {
                lhs.remove(i);
                rhs.remove(i);
            }
        }
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
    public IND get(int index) {
        return new IND(lhs.get(index), rhs.get(index));
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
