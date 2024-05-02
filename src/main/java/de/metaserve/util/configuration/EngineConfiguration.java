package de.metaserve.util.configuration;

import de.metaserve.util.singletons.ExecutorConfigurationSingelton;
import de.metaserve.util.singletons.InputConfigurationSingleton;
import de.metaserve.util.singletons.ParserConfigurationSingleton;

import java.io.*;
import java.util.Properties;

public class EngineConfiguration implements Configuration{
    private boolean LOG_DATA = false;
    private int TIME_OUT_LIMIT = -1;
    private int MEMORY_LIMIT = -1;
    private boolean CACHE = false;

    private ParserConfiguration parserConfig;
    private ExecutorConfiguration executorConfig;
    private InputConfiguration inputConfiguration;

    public EngineConfiguration(){
        this.parserConfig = ParserConfigurationSingleton.get();
        this.executorConfig = ExecutorConfigurationSingelton.get();
        this.inputConfiguration = InputConfigurationSingleton.get();
        try {
            this.load();
        } catch (RuntimeException e) {
            throw new RuntimeException("Could not load Engine config", e);
        }
    }

    @Override
    public Properties saveClassToProperties() {
        Properties config = loadPropertiesFromFile();
        config.setProperty("LOG_DATA", String.valueOf(LOG_DATA));
        config.setProperty("TIME_OUT_LIMIT", String.valueOf(TIME_OUT_LIMIT));
        config.setProperty("MEMORY_LIMIT", String.valueOf(MEMORY_LIMIT));
        config.setProperty("CACHE", String.valueOf(CACHE));
        return config;
    }

    @Override
    public void loadClassFromProperties(Properties config) {
        LOG_DATA = Boolean.parseBoolean(config.getProperty("LOG_DATA"));
        TIME_OUT_LIMIT = Integer.parseInt(config.getProperty("TIME_OUT_LIMIT"));
        MEMORY_LIMIT = Integer.parseInt(config.getProperty("MEMORY_LIMIT"));
        CACHE = Boolean.parseBoolean(config.getProperty("CACHE"));
    }

    public ParserConfiguration getParserConfig(){
        return parserConfig;
    }

    public InputConfiguration getInputConfig(){
        return inputConfiguration;
    }

    public ExecutorConfiguration getExecutorConfig(){
        return executorConfig;
    }

    public boolean isLog() {
        return LOG_DATA;
    }

    public int getTimeoutLimit() {
        return TIME_OUT_LIMIT;
    }

    public int getMemoryLimit() {
        return MEMORY_LIMIT;
    }

    public boolean isCache() {
        return CACHE;
    }

    public EngineConfiguration setCache(boolean cache) {
        this.CACHE = cache;
        return this;
    }

    public EngineConfiguration setLog(boolean log) {
        this.LOG_DATA = log;
        return this;
    }

    public EngineConfiguration setTimeoutLimit(int timeoutLimit) {
        this.TIME_OUT_LIMIT = timeoutLimit;
        return this;
    }

    public EngineConfiguration setMemoryLimit(int memoryLimit) {
        this.MEMORY_LIMIT = memoryLimit;
        return this;
    }

    public EngineConfiguration setParserConfig(ParserConfiguration parserConfig) {
        this.parserConfig = parserConfig;
        return this;
    }

    public EngineConfiguration setExecutorConfig(ExecutorConfiguration executorConfig) {
        this.executorConfig = executorConfig;
        return this;
    }

    public EngineConfiguration setInputConfiguration(InputConfiguration inputConfiguration) {
        this.inputConfiguration = inputConfiguration;
        return this;
    }

    public void close() {
        try {
            save();
            parserConfig.save();
            executorConfig.save();
            inputConfiguration.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
