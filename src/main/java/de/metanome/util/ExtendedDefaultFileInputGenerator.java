package de.metanome.util;

import de.metanome.algorithm_integration.AlgorithmConfigurationException;
import de.metanome.algorithm_integration.configuration.ConfigurationSettingFileInput;
import de.metanome.algorithm_integration.input.InputGenerationException;
import de.metanome.algorithm_integration.input.InputIterationException;
import de.metanome.algorithm_integration.input.RelationalInput;
import de.metanome.backend.input.file.DefaultFileInputGenerator;
import de.metanome.backend.input.file.FileIterator;
import jnr.ffi.annotations.In;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;

public class ExtendedDefaultFileInputGenerator extends DefaultFileInputGenerator{

     Charset charset = Charset.defaultCharset();

    protected ExtendedDefaultFileInputGenerator() {}

    public ExtendedDefaultFileInputGenerator(File inputFile) throws FileNotFoundException {
        super(inputFile);
    }

    public ExtendedDefaultFileInputGenerator(ExtendedConfigurationSettingFileInput setting)
            throws AlgorithmConfigurationException {
        super(setting);
        if (setting.charset != null) this.charset = setting.charset;
    }
    public ExtendedDefaultFileInputGenerator(File inputFile, ExtendedConfigurationSettingFileInput setting)
            throws AlgorithmConfigurationException, FileNotFoundException {
        super(inputFile, setting);
        if (setting.charset != null) this.charset = setting.charset;
    }

    public RelationalInput generateNewCopy() throws InputGenerationException {
        try {
            return new FileIterator(getInputFile().getName(), new FileReader(getInputFile(), charset), setting);
        } catch (FileNotFoundException e) {
            throw new InputGenerationException("File not found!", e);
        } catch (InputIterationException e) {
            throw new InputGenerationException("Could not iterate over the first line of the file input", e);
        } catch (IOException e) {
            throw new InputGenerationException("Charset problems!", e);
        }
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }
}
