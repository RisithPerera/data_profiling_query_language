package de.metaserve.util.configuration;

import java.util.Properties;

public class ExecutorConfiguration implements Configuration{

    private Executor EXECUTOR_TYPE = Executor.DPAL;
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
        properties.setProperty("EXECUTOR_TYPE", EXECUTOR_TYPE.name());
        properties.setProperty("OUTPUT_TYPE", OUTPUT_TYPE.name());
        return properties;
    }

    @Override
    public void loadClassFromProperties(Properties config) {
        EXECUTOR_TYPE = Executor.valueOf(config.getProperty("EXECUTOR_TYPE"));
        OUTPUT_TYPE = Output.valueOf(config.getProperty("OUTPUT_TYPE"));
    }

    public Executor getExecutorType() {
        return EXECUTOR_TYPE;
    }

    public void setExecutorType(Executor EXECUTOR_TYPE) {
        this.EXECUTOR_TYPE = EXECUTOR_TYPE;
    }

    public Output getOutputType() {
        return OUTPUT_TYPE;
    }

    public void setOutputType(Output outputType) {
        this.OUTPUT_TYPE = outputType;
    }

    public enum Executor{
        DPAL,
        HOLISTIC
    }

    public enum Output{
        DEFAULT,
        CONSOLE,
        FILE,
        PRIMITIVE
    }
}
