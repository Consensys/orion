package net.consensys.orion.api.enclave;

import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.api.exception.OrionException;

public class EnclaveException extends OrionException {

  public EnclaveException(OrionErrorCode code, String message) {
    super(code, message);
  }

  public EnclaveException(OrionErrorCode code, Throwable cause) {
    super(code, cause);
  }
}
