package de.metaserve.parser.graph;

import de.metaserve.util.result.ResultSet;

import java.util.Set;

public interface Mergeable {
    String getLeft();
    String getRight();

    default boolean applies(Set<String> names, Set<String> names1){
        return (
                    (names.contains(getLeft()) &&
                    (!names.contains(getRight())) &&
                    (!names1.contains(getLeft())) &&
                    names1.contains(getRight()))
                ||
                    (names.contains(getRight()) &&
                    (!names.contains(getLeft())) &&
                    (!names1.contains(getRight())) &&
                    names1.contains(getLeft()))
        );
    }

    void merge(ResultSet baseSet, ResultSet lists);
}
