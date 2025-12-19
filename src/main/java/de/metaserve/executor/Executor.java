package de.metaserve.executor;

import de.metaserve.parser.query.Query;
import de.metaserve.util.result.ResultSet;
import de.metaserve.util.configuration.ExecutorConfiguration;

import java.util.List;

public interface Executor {

    static Executor get(ExecutorConfiguration inputConfig) {
        return switch (inputConfig.getExecutorType()) {
            case DPAL -> new DPALExecutor(inputConfig);
            case HOLISTIC -> new HolisticExecutor(inputConfig);
        };
    }

    List<ResultSet> executeQuery(Query query);


}
