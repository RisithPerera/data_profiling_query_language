package de.metaserve.optimizer;

import de.metaserve.parser.query.Query;

public class DefaultOptimizer implements Optimizer {

    @Override
    public Query optimize(Query query) {
        return query;
    }
}
