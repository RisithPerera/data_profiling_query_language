package de.metathesis.profilers.requests;

import de.metathesis.structures.AttributeBitSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 * Represents a closed set of valid search space variants.
 * <p>
 * Sealing guarantees that a search space is either:
 *  - Free: defined by relations and a specific level (unlocked, generated space), or
 *  - Lock: defined by a fixed set of attribute combinations.
 * <p>
 * This removes illegal state combinations, eliminates boolean flags and null checks,
 * and enables exhaustive handling via pattern matching at compile time.
 * <p>
 * Author: Risith Perera
 * Date: 2025-12-31
 */

public sealed interface SearchSpace permits SearchSpace.Free, SearchSpace.Lock {

    record Free(int[] relations, int level) implements SearchSpace {}

    record Lock(ObjectOpenHashSet<AttributeBitSet> attributes) implements SearchSpace {}
}
