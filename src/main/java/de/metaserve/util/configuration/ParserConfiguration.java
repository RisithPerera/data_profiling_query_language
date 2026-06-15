package de.metaserve.util.configuration;


import java.util.Properties;

public class ParserConfiguration implements Configuration{

    private Parser PARSER_TYPE = Parser.ANTLR;

    public ParserConfiguration(){
        try {
            this.load();
        } catch (RuntimeException e) {
            throw new RuntimeException("Could not load Parser config", e);
        }
    }

    @Override
    public Properties saveClassToProperties() {
        Properties config = loadPropertiesFromFile();
        config.setProperty("PARSER_TYPE", PARSER_TYPE.name());
        return config;
    }

    @Override
    public void loadClassFromProperties(Properties config) {
        PARSER_TYPE = Parser.valueOf(config.getProperty("PARSER_TYPE"));
    }

    public enum Parser {
        ANTLR,
    }

    public Parser getParser(){
        return PARSER_TYPE;
    }
}
