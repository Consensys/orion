package net.consensys.orion.impl.enclave.sodium;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import net.consensys.orion.api.enclave.EncryptedPayload;

import javax.xml.bind.DatatypeConverter;

import org.junit.jupiter.api.Test;

class SodiumEnclaveStubTest {

  @Test
  void roundTripEncryption() {
    byte[] message = "hello".getBytes(UTF_8);
    SodiumEnclaveStub enclave = new SodiumEnclaveStub();
    EncryptedPayload encryptedPayload = enclave.encrypt(message, null, null);
    byte[] bytes = enclave.decrypt(encryptedPayload, null);
    assertArrayEquals(message, bytes);
  }

  @Test
  void roundTripEncryptionWithFunkyBytes() {
    byte[] message = DatatypeConverter.parseHexBinary("0079FF00FF89");
    SodiumEnclaveStub enclave = new SodiumEnclaveStub();
    EncryptedPayload encryptedPayload = enclave.encrypt(message, null, null);
    byte[] bytes = enclave.decrypt(encryptedPayload, null);
    assertArrayEquals(message, bytes);
  }
}
