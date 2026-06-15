package de.metaserve.input;

import de.metaserve.util.exceptions.AlgorithmConfigurationException;
import de.metaserve.util.exceptions.InputGenerationException;

public interface RelationalInputGenerator extends AutoCloseable {
    RelationalInput generateNewCopy() throws InputGenerationException, AlgorithmConfigurationException;
}
