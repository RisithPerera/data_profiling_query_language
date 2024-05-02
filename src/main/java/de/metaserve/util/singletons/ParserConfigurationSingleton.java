package de.metaserve.util.singletons;

import de.metaserve.util.configuration.ParserConfiguration;

public class ParserConfigurationSingleton {

    private static ParserConfiguration singleton = new ParserConfiguration();

    public static ParserConfiguration get() {
        return singleton;
    }

    public static void set(ParserConfiguration instance) {
        singleton = instance;
    }
}
