package de.metaserve.util.exceptions;


public class AlgorithmConfigurationException extends AlgorithmExecutionException {

  private static final long serialVersionUID = 846178386204773632L;

  public AlgorithmConfigurationException() {
    super();
  }

  public AlgorithmConfigurationException(String message) {
    super(message);
  }

  public AlgorithmConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

}
