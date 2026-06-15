package de.metaserve.input;

import de.metaserve.util.exceptions.AlgorithmConfigurationException;
import de.metaserve.util.exceptions.InputGenerationException;
import de.metaserve.util.exceptions.InputIterationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.charset.Charset;

public class DefaultFileInputGenerator implements FileInputGenerator {

    protected ConfigurationSettingFileInput setting;
    File inputFile;
    Charset charset = Charset.defaultCharset();

    protected DefaultFileInputGenerator() {
    }

    /**
     * @param setting the settings to construct new {@link de.metanome.algorithm_integration.input.RelationalInput}s
     *                with
     * @throws AlgorithmConfigurationException thrown if the file cannot be found
     */
    public DefaultFileInputGenerator(ConfigurationSettingFileInput setting)
            throws AlgorithmConfigurationException {
        try {
            this.setInputFile(new File(setting.getFileName()));
        } catch (FileNotFoundException e) {
            throw new AlgorithmConfigurationException("File not found!", e);
        }
        this.setting = setting;
    }

    public DefaultFileInputGenerator(File inputFile, ConfigurationSettingFileInput setting)
            throws AlgorithmConfigurationException, FileNotFoundException {
        try {
            this.setInputFile(inputFile);
        } catch (FileNotFoundException e) {
            throw new AlgorithmConfigurationException("File not found!", e);
        }
        this.setting = setting;
    }

    @Override
    public RelationalInput generateNewCopy() throws InputGenerationException {
        try {
            return new FileIterator(inputFile.getName(), new FileReader(inputFile), setting);
        } catch (FileNotFoundException e) {
            throw new InputGenerationException("File not found!", e);
        } catch (InputIterationException e) {
            throw new InputGenerationException("Could not iterate over the first line of the file input", e);
        }
    }


    @Override
    public File getInputFile() {
        return inputFile;
    }

    private void setInputFile(File inputFile) throws FileNotFoundException {
        if (inputFile.isFile()) {
            this.inputFile = inputFile;
        } else {
            throw new FileNotFoundException();
        }
    }

    public ConfigurationSettingFileInput getSetting() {
        return this.setting;
    }

    @Override
    public void close() throws Exception {
        // Nothing to close
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }
}