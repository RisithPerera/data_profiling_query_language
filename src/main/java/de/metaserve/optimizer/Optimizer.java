package de.metaserve.optimizer;

import de.metaserve.parser.query.Query;

public interface Optimizer {

    static Optimizer get() {
        return new DefaultOptimizer();
    }

    Query optimize(Query query);

}
