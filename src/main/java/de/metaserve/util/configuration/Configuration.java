package de.metaserve.util.configuration;

import java.io.*;
import java.util.Properties;

public interface Configuration {
    default void load() throws RuntimeException {
        Properties properties = this.loadPropertiesFromFile(this.getConfigFilePath());
        this.loadClassFromProperties(properties);
    }

    default Properties loadPropertiesFromFile(){
        return this.loadPropertiesFromFile(this.getConfigFilePath());
    }

    default Properties loadPropertiesFromFile(String filePath) {
        Properties config = new Properties();
        try(InputStream input = new FileInputStream(filePath)) {
            config.load(input);
        } catch (IOException e){
            e.printStackTrace();
        }
        return config;
    }

    default void save() throws IOException{
        Properties properties = this.saveClassToProperties();
        this.savePropertiesToFile(getConfigFilePath(), properties);
    }

    default void savePropertiesToFile(String fileName, Properties properties) throws IOException {
        try (OutputStream out = new FileOutputStream(fileName)) {
            properties.store(out, null);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    Properties saveClassToProperties();

    void loadClassFromProperties(Properties config);

    default String getConfigFilePath(){
        return InputConfiguration.path/*.substring(0, System.getProperty("user.dir").lastIndexOf(File.separator)) */+ File.separator + "io" + File.separator + "configurations" + File.separator + "engine.properties";
    }

}
