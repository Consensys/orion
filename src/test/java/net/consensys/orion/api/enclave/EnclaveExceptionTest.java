package net.consensys.orion.api.enclave;

import static org.junit.Assert.*;

import org.junit.Test;

public class EnclaveExceptionTest {

  @Test
  public void implementationOfRuntimeInterface() {
    assertTrue(RuntimeException.class.isAssignableFrom(EnclaveException.class));
  }

  @Test
  public void construction() {
    String message = "This is the cause";
    EnclaveException exception = new EnclaveException(message);
    assertEquals(message, exception.getMessage());
    Throwable cause = new Throwable();
    EnclaveException anotherException = new EnclaveException(message, cause);
    assertEquals(message, anotherException.getMessage());
    assertEquals(cause, anotherException.getCause());
  }
}
