package de.metathesis.structures.results;

import de.metathesis.structures.AttributeBitSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class UCCResult implements Result<UCCResult.UCC>{

    private final ObjectArrayList<AttributeBitSet> lhs = new ObjectArrayList<>();

    public boolean add(AttributeBitSet lhsBitSet) {
        int index = lhs.indexOf(lhsBitSet);
        if (index >= 0) {
            return false;
        }

        return lhs.add(lhsBitSet);
    }

    @Override
    public void cropByLhsSet(ObjectOpenHashSet<AttributeBitSet> lhsSet) {
        for (int i = lhs.size() - 1; i >= 0; i--) {
            if (!lhsSet.contains(lhs.get(i))) {
                lhs.remove(i);
            }
        }
    }

    @Override
    public void cropByRhsSet(ObjectOpenHashSet<AttributeBitSet> rhsSet) {
        for (int i = lhs.size() - 1; i >= 0; i--) {
            if (!rhsSet.contains(lhs.get(i))) {
                lhs.remove(i);
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
    public UCC get(int index) {
        return new UCC(lhs.get(index));
    }

    @Override
    public ObjectOpenHashSet<AttributeBitSet> asLhsSet() {
        return new ObjectOpenHashSet<>(lhs);
    }

    @Override
    public ObjectOpenHashSet<AttributeBitSet> asRhsSet() {
        return new ObjectOpenHashSet<>(lhs); //Return Same
    }

    /* --- Separate Iteration --- */
    @Override
    public Iterable<AttributeBitSet> lhs() {
        return lhs;
    }

    @Override
    public Iterable<AttributeBitSet> rhs() {
        return lhs; //Return Same
    }

    @Override
    public Iterator<UCC> iterator() {
        Iterator<AttributeBitSet> it = lhs.iterator();

        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public UCC next() {
                if (!hasNext()) throw new NoSuchElementException();
                return new UCC(it.next());
            }
        };
    }

    /* --- Internal UCC Object --- */
    public static final class UCC {
        public final AttributeBitSet lhs;

        private UCC(AttributeBitSet lhs) {
            this.lhs = lhs;
        }

        @Override
        public String toString() {
            return lhs.toString();
        }
    }
}
