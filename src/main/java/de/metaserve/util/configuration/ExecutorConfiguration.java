package de.metaserve.util.configuration;

import java.io.IOException;
import java.util.Properties;

public class ExecutorConfiguration implements Configuration{

    private Output OUTPUT_TYPE = Output.DEFAULT;

    public ExecutorConfiguration(){
        try {
            this.load();
        } catch (RuntimeException e) {
            throw new RuntimeException("Could not load Executor config", e);
        }
    }

    @Override
    public Properties saveClassToProperties() {
        Properties properties = loadPropertiesFromFile();
        properties.setProperty("OUTPUT_TYPE", OUTPUT_TYPE.name());
        return properties;
    }

    @Override
    public void loadClassFromProperties(Properties config) {
        OUTPUT_TYPE = Output.valueOf(config.getProperty("OUTPUT_TYPE"));
    }

    public Output getOutputType() {
        return OUTPUT_TYPE;
    }

    public void setOutputType(Output outputType) {
        this.OUTPUT_TYPE = outputType;
    }

    public enum Output{
        DEFAULT,
        CONSOLE,
        FILE,
        PRIMITIVE
    }
}
