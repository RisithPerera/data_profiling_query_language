package de.metathesis.structures;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PositionListIndexTest {

    @Test
    public void testIntersectionCorrectness() {
        PositionListIndex pli1 = new PositionListIndex(new AttributeBitSet(0, 0), List.of(cluster(0,1,2,5), cluster(3,4)));
        PositionListIndex pli2 = new PositionListIndex(new AttributeBitSet(0, 1), List.of(cluster(0,1), cluster(2,3,4,5)));
        PositionListIndex pli3 = new PositionListIndex(new AttributeBitSet(0, 2), List.of(cluster(0,1), cluster(2,5), cluster(3,4)));
        PositionListIndex pli4 = new PositionListIndex(new AttributeBitSet(0, 3), Collections.emptyList());
        PositionListIndex pli5 = new PositionListIndex(new AttributeBitSet(0, 4), List.of(cluster(0,2,4), cluster(1,5)));
        PositionListIndex pli6 = new PositionListIndex(new AttributeBitSet(0, 5), List.of(cluster(0,2), cluster(1,5), cluster(3,4)));
        PositionListIndex pli7 = new PositionListIndex(new AttributeBitSet(0, 6), List.of(cluster(3,4)));

        assertEquals(pli1.getClusters(), pli1.intersect(pli1).getClusters());
        assertEquals(pli3.getClusters(), pli1.intersect(pli2).getClusters());
        assertEquals(pli3.getClusters(), pli2.intersect(pli1).getClusters());
        assertEquals(pli4.getClusters(), pli1.intersect(pli2).intersect(pli5).getClusters());
        assertEquals(pli7.getClusters(), pli1.intersect(pli6).intersect(pli3).getClusters());
    }

    private IntArrayList cluster(int... rows) {
        return new IntArrayList(rows);
    }
}
