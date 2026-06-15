package de.metaserve.engine;

import de.metaserve.executor.Executor;
import de.metaserve.optimizer.Optimizer;
import de.metaserve.parser.Parser;
import de.metaserve.parser.query.Query;
import de.metaserve.util.configuration.EngineConfiguration;
import de.metaserve.util.exceptions.DPQLException;
import de.metaserve.util.listener.Listenable;
import de.metaserve.util.listener.QueryExecutionListener;
import de.metaserve.util.result.ResultSet;
import de.metaserve.util.singletons.EngineConfigurationSingleton;
import de.metathesis.structures.ResultTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An implementation of the QueryEngine interface that parses, optimizes and executes DPQL queries.
 */
public class Metaserve implements QueryEngine, AutoCloseable, Listenable<QueryExecutionListener> {
    /**
     * The current state of the query engine.
     */
    private volatile QueryState state = QueryState.NOT_INITIALIZED;

    /**
     * The engine configuration used by the query engine.
     */
    private final EngineConfiguration config;

    /**
     * The parser used by the query engine.
     */
    private final Parser parser;

    /**
     * The optimizer used by the query engine.
     */
    private final Optimizer optimizer;

    /**
     * The executor used by the query engine.
     */
    private final Executor executor;

    /**
     * All listeners that get notified about results from the engine.
     */
    private final List<QueryExecutionListener> executionListeners;

    /**
     * Creates a new instance of the Query Engine with the given configuration.
     * @param config the configuration for the Query Engine.
     */
    public Metaserve(EngineConfiguration config) {
        this.state = QueryState.AWAITING_QUERY;
        this.config = Objects.requireNonNull(config, "Configuration cannot be null!");
        this.parser = Parser.get(config.getParserConfig());
        this.optimizer = Optimizer.get();
        this.executor = Executor.get(config.getExecutorConfig());
        this.executionListeners = new ArrayList<>();
    }

    /**
     * Creates a new instance of the Query Engine with default configuration.
     */
    public Metaserve() {
        this(EngineConfigurationSingleton.get());
    }

    @Override
    public List<ResultTable> executeQuery(String queryString) throws DPQLException {
        if (!(state.equals(QueryState.AWAITING_QUERY) || state.equals(QueryState.QUERY_COMPLETED) || state.equals(QueryState.ERROR))) {
            throw new DPQLException("Engine is currently occupied!");
        }

        state = QueryState.QUERY_PARSING;
        try {
            long time = System.currentTimeMillis();
            Query query = parser.parse(queryString);
            query.getMetaData().setTime(time);
            query.getMetaData().update(QueryState.QUERY_PARSING);

            state = QueryState.QUERY_OPTIMIZING;
            Query optimizedQuery = optimizer.optimize(query);
            query.getMetaData().update(QueryState.QUERY_OPTIMIZING);

            state = QueryState.QUERY_EXECUTING;
            List<ResultTable> resultSet = executor.executeQuery(optimizedQuery);
            if(resultSet == null) {
                throw new RuntimeException("Query execution failed, due to a missing result set!");
            }
            query.getMetaData().update(QueryState.QUERY_EXECUTING);
            state = QueryState.QUERY_COMPLETED;

            // Notify all listeners of the query execution completion
            for (QueryExecutionListener listener : executionListeners) {
                listener.onQueryCompleted(optimizedQuery, resultSet, optimizedQuery.getMetaData().getTime(), 0);
            }

            return resultSet;
        } catch (Exception e) {
            state = QueryState.ERROR;
            throw new DPQLException("Query execution failed!",e);
        }
    }

    @Override
    public void close() throws DPQLException {
        if (state == QueryState.CLOSED) {
            throw new DPQLException("Engine is already closed!");
        }

        try {
            parser.close();
            config.close();
            // Notify all listeners of the engine closure
            for (QueryExecutionListener listener : executionListeners) {
                listener.onEngineClosed();
            }
        } catch (Exception e) {
            throw new DPQLException("Could not close all resources from the engine!");
        } finally {
            state = QueryState.CLOSED;
        }
    }

    @Override
    public QueryState getState() {
        return state;
    }

    @Override
    public EngineConfiguration getConfig(){
        return config;
    }

    @Override
    public List<QueryExecutionListener> getListeners() {
        return executionListeners;
    }
}
