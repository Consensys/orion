package net.consensys.orion.api.storage;

public class StorageException extends RuntimeException {

  public StorageException(String message) {
    super(message);
  }

  public StorageException(String message, Throwable cause) {
    super(message, cause);
  }

  public StorageException(Throwable cause) {
    super(cause);
  }

  protected StorageException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
