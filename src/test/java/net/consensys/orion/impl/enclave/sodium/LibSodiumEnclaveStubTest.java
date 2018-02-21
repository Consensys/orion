package net.consensys.orion.impl.enclave.sodium;

import static org.junit.Assert.*;

import net.consensys.orion.api.enclave.EncryptedPayload;

import javax.xml.bind.DatatypeConverter;

import org.junit.Test;

public class LibSodiumEnclaveStubTest {

  @Test
  public void testRoundTripEncryption() {
    byte[] message = "hello".getBytes();
    LibSodiumEnclaveStub enclave = new LibSodiumEnclaveStub();
    EncryptedPayload encryptedPayload = enclave.encrypt(message, null, null);
    byte[] bytes = enclave.decrypt(encryptedPayload, null);
    assertArrayEquals(message, bytes);
  }

  @Test
  public void testRoundTripEncryptionWithFunkyBytes() {
    byte[] message = DatatypeConverter.parseHexBinary("0079FF00FF89");
    LibSodiumEnclaveStub enclave = new LibSodiumEnclaveStub();
    EncryptedPayload encryptedPayload = enclave.encrypt(message, null, null);
    byte[] bytes = enclave.decrypt(encryptedPayload, null);
    assertArrayEquals(message, bytes);
  }
}
