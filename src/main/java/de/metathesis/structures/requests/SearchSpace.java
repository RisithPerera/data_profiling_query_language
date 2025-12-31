package de.metathesis.structures.requests;

import de.metathesis.structures.AttributeBitSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Represents a closed set of valid search space variants.
 * <p>
 * Sealing guarantees that a search space is either:
 *  - CC: defined by relations and a level (unlocked, generated space), or
 *  - Locked: defined by a fixed set of attribute combinations.
 * <p>
 * This removes illegal state combinations, eliminates boolean flags and null checks,
 * and enables exhaustive handling via pattern matching at compile time.
 * <p>
 * Author: Risith Perera
 * Date: 2025-12-31
 */

public sealed interface SearchSpace permits SearchSpace.CC, SearchSpace.Locked {

    record CC(int[] relations, int level) implements SearchSpace {}

    record Locked(ObjectArrayList<AttributeBitSet> attributes) implements SearchSpace {}
}
