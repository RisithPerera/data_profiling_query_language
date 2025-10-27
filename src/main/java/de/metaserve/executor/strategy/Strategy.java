package de.metaserve.executor.strategy;

import de.metanome.Metanome;
import de.metaserve.executor.min.graph.Graph;
import de.metaserve.parser.query.QueryMetadata;

public interface Strategy {

    static void apply(Graph graph, QueryMetadata metadata) {
        Metanome metanome = Metanome.getInstance();
        PreProfileStrategy preProfileStrategy = new PreProfileStrategy(graph, metadata);
        preProfileStrategy.apply(metanome);
    }

    void apply(Metanome metanome);
}
