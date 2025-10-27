package de.metaserve.executor;

import de.metaserve.parser.query.Query;
import de.metaserve.model.result.ResultSet;
import de.metaserve.util.configuration.ExecutorConfiguration;

import java.util.List;

public interface Executor {

    static Executor get(ExecutorConfiguration inputConfig) {
        return new DPALExecutor(inputConfig);
    }

    List<ResultSet> executeQuery(Query query);


}
