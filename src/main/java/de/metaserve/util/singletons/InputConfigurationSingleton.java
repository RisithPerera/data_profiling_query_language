package de.metaserve.util.singletons;

import de.metaserve.util.configuration.InputConfiguration;

public class InputConfigurationSingleton {

	private static InputConfiguration singleton = new InputConfiguration();

	public static InputConfiguration get() {
		return singleton;
	}

	public static void set(InputConfiguration instance) {
		singleton = instance;
	}
}
