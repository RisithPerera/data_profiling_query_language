package de.metanome;

import de.metanome.algorithm_integration.results.Result;
import de.metaserve.util.singletons.EngineConfigurationSingleton;

import java.util.List;

public interface Metanome {

    static Metanome getInstance() {
        if(EngineConfigurationSingleton.get().isCache())
            return MetanomeCache.getInstance();
        return MetanomeImpl.getInstance();
    }

    List<Result> executeUCC(String... fileNames);

    List<Result> executeFD(String... fileNames);

    List<Result> executeIND(String... fileNames);

    List<Result> executesIND(String... fileNames);

    List<Result> executeCARD(String... fileNames);
}
