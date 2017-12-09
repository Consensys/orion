package net.consensys.athena.api.enclave;

public class EnclaveException extends RuntimeException {

  public EnclaveException(String message) {
    super(message);
  }

  public EnclaveException(String message, Throwable cause) {
    super(message, cause);
  }

  public EnclaveException(Throwable cause) {
    super(cause);
  }

  protected EnclaveException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
