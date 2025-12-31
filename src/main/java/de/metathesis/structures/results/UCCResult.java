package de.metathesis.structures.results;

import de.metathesis.structures.AttributeBitSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Iterator;

public final class UCCResult implements Iterable<AttributeBitSet> {

    private final ObjectArrayList<AttributeBitSet> lhs = new ObjectArrayList<>();

    public void add(AttributeBitSet attributeBitSet) {
        lhs.add(attributeBitSet);
    }

    public int size() {
        return lhs.size();
    }

    public boolean isEmpty() {
        return lhs.isEmpty();
    }

    public AttributeBitSet get(int index) {
        return lhs.get(index);
    }

    public ObjectArrayList<AttributeBitSet> asLhsList() {
        return lhs;
    }

    @Override
    public Iterator<AttributeBitSet> iterator() {
        return lhs.iterator();
    }
}
