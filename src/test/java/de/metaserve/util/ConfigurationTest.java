package de.metaserve.util;

import de.metaserve.util.configuration.EngineConfiguration;
import de.metaserve.util.singletons.EngineConfigurationSingleton;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class ConfigurationTest {
    int numberOfProperties = 28;

    @Test
    public void loadConfig(){
        EngineConfiguration engineConfiguration = EngineConfigurationSingleton.get();
        Properties config = engineConfiguration.loadPropertiesFromFile();
        Assert.assertEquals(numberOfProperties, config.size());
    }

    @Test
    public void saveConfig(){
        EngineConfiguration engineConfiguration = EngineConfigurationSingleton.get();
        engineConfiguration.loadPropertiesFromFile();
        engineConfiguration.close();
        Properties config = engineConfiguration.loadPropertiesFromFile();
        Assert.assertEquals(numberOfProperties, config.size());
    }
}
