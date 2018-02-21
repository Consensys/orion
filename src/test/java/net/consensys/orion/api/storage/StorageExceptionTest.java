package net.consensys.orion.api.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StorageExceptionTest {

  @Test
  public void testImplementationOfRuntimeInterface() {
    assertTrue(RuntimeException.class.isAssignableFrom(StorageException.class));
  }

  @Test
  public void testConstruction() {
    String message = "This is the cause";
    StorageException exception = new StorageException(message);
    assertEquals(message, exception.getMessage());
    Throwable cause = new Throwable();
    StorageException anotherException = new StorageException(message, cause);
    assertEquals(message, anotherException.getMessage());
    assertEquals(cause, anotherException.getCause());
  }
}
