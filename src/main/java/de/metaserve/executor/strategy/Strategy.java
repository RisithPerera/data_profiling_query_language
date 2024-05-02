package de.metaserve.executor.strategy;

import de.metanome.Metanome;
import de.metaserve.model.dpal.Graph;
import de.metaserve.model.query.QueryMetadata;

public interface Strategy {

    static void apply(Graph graph, QueryMetadata metadata) {
        Metanome metanome = Metanome.getInstance();
        PreProfileStrategy preProfileStrategy = new PreProfileStrategy(graph, metadata);
        preProfileStrategy.apply(metanome);
    }

    void apply(Metanome metanome);
}
