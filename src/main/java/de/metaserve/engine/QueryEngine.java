package de.metaserve.engine;

import de.metaserve.util.result.ResultSet;
import de.metaserve.util.configuration.EngineConfiguration;
import de.metaserve.util.exceptions.DPQLException;

import java.util.List;


/**
 *  Interface for a query engine.
 */
public interface QueryEngine {

    /**
     *  Executes the given query and returns the result set.
     *  @param queryString the query to execute
     *  @return the results set of the query
     *  @throws DPQLException if the query engine encounters an error while executing the query
     */
    List<ResultSet> executeQuery(String queryString) throws DPQLException;

    /**
     *  Closes the query engine.
     *  @throws DPQLException if the query engine encounters an error while closing
     */
    void close() throws DPQLException;

    /**
     *  Returns the current state of the query engine.
     *  @return the current state of the query engine
     */
    QueryState getState();

    /**
     * Returns the engine configuration object used by this query engine.
     * @return The engine configuration object used by this query engine.
     */
    EngineConfiguration getConfig();

    /**
     *  Enumeration of possible states of the query engine.
     */
    enum QueryState {
        NOT_INITIALIZED, // the query engine has not been initialized yet
        AWAITING_QUERY, // the query engine is waiting for a query to be executed
        QUERY_EXECUTING, // the query engine is currently executing a query
        QUERY_PARSING, // the query engine is currently parsing a query
        QUERY_OPTIMIZING, // the query engine is currently optimizing a query
        QUERY_WAITING_FOR_METANOME, // the query engine is currently waiting for Metanom
        QUERY_RESULT,
        COMPUTED_MIN,
        QUERY_CANCELLED, // the query engine has cancelled the execution of the current query
        QUERY_COMPLETED, // the query engine has completed the execution of the current query
        QUERY_PAUSED, // the query engine has paused the execution of the current query
        ERROR, // the query engine has encountered an error
        CLOSED // the query engine has been closed and cannot be used anymore
        ;
    }
}
