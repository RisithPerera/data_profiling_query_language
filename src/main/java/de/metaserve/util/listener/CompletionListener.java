package de.metaserve.util.listener;

import de.metaserve.engine.QueryEngine;
import de.metaserve.parser.query.Query;

public interface CompletionListener extends QueryExecutionListener {

    default void onEvent(String event){}
    default void onEvent(QueryEngine.QueryState event, Query query){}
    default void onEngineClosed(){}
}
