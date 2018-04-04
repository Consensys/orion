package net.consensys.orion.api.cmd;

public class OrionStartException extends RuntimeException {

  public OrionStartException(String message) {
    super(message);
  }

  public OrionStartException(String message, Throwable cause) {
    super(message, cause);
  }
}
