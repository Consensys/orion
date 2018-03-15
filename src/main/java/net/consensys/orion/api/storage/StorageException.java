package net.consensys.orion.api.storage;

import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.api.exception.OrionException;

public class StorageException extends OrionException {

  public StorageException(OrionErrorCode code, String message) {
    super(code, message);
  }

  public StorageException(OrionErrorCode code, Throwable cause) {
    super(code, cause);
  }
}
