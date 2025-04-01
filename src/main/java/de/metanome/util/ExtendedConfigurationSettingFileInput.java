package de.metanome.util;

import de.metanome.algorithm_integration.configuration.ConfigurationSettingFileInput;

import java.nio.charset.Charset;

public class ExtendedConfigurationSettingFileInput extends ConfigurationSettingFileInput {

    Charset charset = null;
    public ExtendedConfigurationSettingFileInput() {}

    public ExtendedConfigurationSettingFileInput(String fileName) {
        super(fileName);
    }

    public ExtendedConfigurationSettingFileInput(String fileName, boolean advanced, char separator,
                                         char quote,
                                         char escape, boolean strictQuotes,
                                         boolean ignoreLeadingWhiteSpace, int line,
                                         boolean header, boolean skipDifferingLines,
                                         String nullValue) {
        super(fileName, advanced, separator, quote, escape, strictQuotes, ignoreLeadingWhiteSpace, line, header, skipDifferingLines, nullValue);
    }

    public ExtendedConfigurationSettingFileInput(String fileName, boolean advanced, char separator,
                                                 char quote,
                                                 char escape, boolean strictQuotes,
                                                 boolean ignoreLeadingWhiteSpace, int line,
                                                 boolean header, boolean skipDifferingLines,
                                                 String nullValue, Charset charset) {
        super(fileName, advanced, separator, quote, escape, strictQuotes, ignoreLeadingWhiteSpace, line, header, skipDifferingLines, nullValue);
        this.charset = charset;
    }
}
