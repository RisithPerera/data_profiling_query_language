
package de.metaserve.util.exceptions;

import java.io.Serializable;


public class AlgorithmExecutionException extends Exception implements Serializable {

  private static final long serialVersionUID = 7685236041703421431L;

  protected AlgorithmExecutionException() {
    super();
  }

  public AlgorithmExecutionException(String message) {
    super(message);
  }

  public AlgorithmExecutionException(String message, Throwable cause) {
    super(message, cause);
  }

}
