package net.consensys.orion.api.exception;

/** Base exception class encapsulating the Orion error codes. */
public class OrionException extends RuntimeException {

  private final OrionErrorCode code;

  public OrionException(OrionErrorCode code) {
    this.code = code;
  }

  public OrionException(OrionErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  public OrionException(OrionErrorCode code, Throwable cause) {
    super(cause);
    this.code = code;
  }

  public OrionErrorCode code() {
    return code;
  }
}
