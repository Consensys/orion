package net.consensys.orion.api.exception;

/**
 * Exception thrown upon starting Orion.
 */
public class OrionStartException extends OrionException {

  public OrionStartException(OrionErrorCode code, String message) {
    super(code, message);
  }
}
