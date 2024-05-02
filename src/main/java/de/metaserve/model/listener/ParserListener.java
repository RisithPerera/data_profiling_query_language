package de.metaserve.model.listener;

import de.metaserve.model.query.Query;

/**
 * A listener interface for events related to query parsing.
 */
public interface ParserListener extends Listener {

    /**
     * Called when the parser has started parsing a query.
     *
     * @param query the query being parsed
     */
    default void onQueryParsingStarted(String query) {}

    /**
     * Called when the parser has successfully parsed a query.
     *
     * @param query the parsed query
     */
    default void onQueryParsed(Query query) {}

    /**
     * Called when an error occurs while parsing a query.
     *
     * @param message the error message
     */
    default void onQueryParsingError(String message) {}
}
