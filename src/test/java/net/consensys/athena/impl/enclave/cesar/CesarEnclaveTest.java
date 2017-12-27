package net.consensys.athena.impl.enclave.cesar;

import static org.junit.Assert.*;

import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.impl.helpers.CesarEnclave;

import javax.xml.bind.DatatypeConverter;

import org.junit.Test;

public class CesarEnclaveTest {

  @Test
  public void testRoundTripEncryption() {
    byte[] message = "hello".getBytes();
    CesarEnclave enclave = new CesarEnclave();
    EncryptedPayload encryptedPayload = enclave.encrypt(message, null, null);
    byte[] bytes = enclave.decrypt(encryptedPayload, null);
    assertArrayEquals(message, bytes);
  }

  @Test
  public void testRoundTripEncryptionWithFunkyBytes() {
    byte[] message = DatatypeConverter.parseHexBinary("0079FF00FF89");
    CesarEnclave enclave = new CesarEnclave();
    EncryptedPayload encryptedPayload = enclave.encrypt(message, null, null);
    byte[] bytes = enclave.decrypt(encryptedPayload, null);
    assertArrayEquals(message, bytes);
  }
}
