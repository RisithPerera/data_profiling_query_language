package de.metaserve.util.exceptions;

public class InputGenerationException extends AlgorithmExecutionException {

  private static final long serialVersionUID = 784025265637319723L;

  public InputGenerationException() {
    super();
  }

  public InputGenerationException(String message) {
    super(message);
  }

  public InputGenerationException(String message, Throwable cause) {
    super(message, cause);
  }

}
