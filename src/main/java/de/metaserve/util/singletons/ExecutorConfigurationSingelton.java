package de.metaserve.util.singletons;

import de.metaserve.util.configuration.ExecutorConfiguration;

public class ExecutorConfigurationSingelton {

    private static ExecutorConfiguration singleton = new ExecutorConfiguration();

    public static ExecutorConfiguration get() {
        return singleton;
    }

    public static void set(ExecutorConfiguration instance) {
        singleton = instance;
    }
}
