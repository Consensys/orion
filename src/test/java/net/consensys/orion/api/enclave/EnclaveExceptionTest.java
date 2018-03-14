package net.consensys.orion.api.enclave;

import static org.junit.Assert.*;

import net.consensys.orion.api.exception.OrionErrorCode;

import org.junit.Test;

public class EnclaveExceptionTest {

  @Test
  public void testImplementationOfRuntimeInterface() {
    assertTrue(RuntimeException.class.isAssignableFrom(EnclaveException.class));
  }

  @Test
  public void testConstruction() {
    final String message = "This is the cause";
    final EnclaveException exception =
        new EnclaveException(OrionErrorCode.ENCLAVE_CREATE_KEY_PAIR, message);
    assertEquals(message, exception.getMessage());
    assertEquals(OrionErrorCode.ENCLAVE_CREATE_KEY_PAIR, exception.code());

    final Throwable cause = new Throwable();
    final EnclaveException anotherException =
        new EnclaveException(OrionErrorCode.ENCLAVE_STORAGE_ENCRYPT, cause);
    assertEquals(cause, anotherException.getCause());
    assertEquals(OrionErrorCode.ENCLAVE_STORAGE_ENCRYPT, anotherException.code());
  }
}
