package de.metaserve.executor;

import de.metaserve.parser.query.Query;
import de.metaserve.util.configuration.ExecutorConfiguration;
import de.metathesis.structures.ResultTable;

import java.util.List;

public interface Executor {

    static Executor get(ExecutorConfiguration inputConfig) {
        return switch (inputConfig.getExecutorType()) {
            case HOLISTIC -> new HolisticExecutor(inputConfig);
        };
    }

    List<ResultTable> executeQuery(Query query);
}
