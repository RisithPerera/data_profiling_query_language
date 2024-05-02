package de.metaserve.executor;

import de.metaserve.model.query.Query;
import de.metaserve.model.query.QueryMetadata;
import de.metaserve.model.result.ResultSet;
import de.metaserve.util.configuration.ExecutorConfiguration;

import java.util.List;

public interface Executor {

    static Executor get(ExecutorConfiguration inputConfig) {
        return new DPALExecutor(inputConfig);
    }

    List<ResultSet> executeQuery(Query query);

    void cancel();

    void pause();

    void resume();

    void close();

}
