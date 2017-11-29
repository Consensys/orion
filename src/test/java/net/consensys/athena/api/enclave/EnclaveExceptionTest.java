package net.consensys.athena.api.enclave;

import static org.junit.Assert.*;

import org.junit.Test;

public class EnclaveExceptionTest {

  @Test
  public void testImplementationOfRuntimeInterface() {
    assertTrue(RuntimeException.class.isAssignableFrom(EnclaveException.class));
  }

  @Test
  public void testConstruction() {
    String message = "This is the cause";
    EnclaveException exception = new EnclaveException(message);
    assertEquals(message, exception.getMessage());
    Throwable cause = new Throwable();
    EnclaveException anotherException = new EnclaveException(message, cause);
    assertEquals(message, anotherException.getMessage());
    assertEquals(cause, anotherException.getCause());
  }
}
