package net.consensys.orion.api.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.consensys.orion.api.exception.OrionErrorCode;

import org.junit.Test;

public class StorageExceptionTest {

  @Test
  public void implementationOfRuntimeInterface() {
    assertTrue(RuntimeException.class.isAssignableFrom(StorageException.class));
  }

  @Test
  public void construction() {
    final String message = "This is the cause";
    final StorageException exception = new StorageException(OrionErrorCode.STORAGE_OPEN, message);
    assertEquals(message, exception.getMessage());
    assertEquals(OrionErrorCode.STORAGE_OPEN, exception.code());

    final Throwable cause = new Throwable();
    final StorageException anotherException =
        new StorageException(OrionErrorCode.STORAGE_CLOSE, cause);
    assertEquals(cause, anotherException.getCause());
    assertEquals(OrionErrorCode.STORAGE_CLOSE, anotherException.code());
  }
}
