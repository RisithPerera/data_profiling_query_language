package de.metaserve.util.singletons;

import de.metaserve.util.configuration.EngineConfiguration;

public class EngineConfigurationSingleton {

    private static EngineConfiguration singleton = new EngineConfiguration();

    public static EngineConfiguration get() {
        return singleton;
    }

    public static void set(EngineConfiguration instance) {
        singleton = instance;
    }
}
