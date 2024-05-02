package de.metaserve.model.listener;

import de.metaserve.engine.QueryEngine;
import de.metaserve.model.query.Query;
import de.metaserve.model.result.ResultSet;

import java.util.List;

/**
 * An interface for listening to query execution events in the query engine.
 */
public interface QueryExecutionListener extends Listener{

    /**
     * Notifies all registered listeners when a query state event occurs.
     * @param event The query state event that occurred.
     * @param query The query associated with the event.
     */
    void onEvent(QueryEngine.QueryState event, Query query);

    /**
     * Called when a query is finished executing.
     *  @param query      The query that finished executing.
     * @param resultSet  The result set produced by the query.
     * @param totalTime  The total time it took to execute the query, in milliseconds.
     * @param resultSize The size of the result set, in number of rows.
     */
    void onQueryCompleted(Query query, List<ResultSet> resultSet, long totalTime, int resultSize);

    /**
     * Called when the query engine is closed.
     */
    void onEngineClosed();
}
