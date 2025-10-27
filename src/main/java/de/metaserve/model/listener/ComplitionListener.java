package de.metaserve.model.listener;

import de.metaserve.engine.QueryEngine;
import de.metaserve.model.query.Query;

public interface ComplitionListener extends QueryExecutionListener {

    default void onEvent(String event){}
    default void onEvent(QueryEngine.QueryState event, Query query){}
    default void onEngineClosed(){}
}
