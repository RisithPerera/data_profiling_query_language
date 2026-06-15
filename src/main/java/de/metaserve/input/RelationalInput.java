package de.metaserve.input;

import de.metaserve.util.exceptions.InputIterationException;

import java.util.List;

public interface RelationalInput extends AutoCloseable {

    boolean hasNext() throws InputIterationException;

    List<String> next() throws InputIterationException;

    int numberOfColumns();

    String relationName();

    List<String> columnNames();
}
