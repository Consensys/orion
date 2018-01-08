package net.consensys.athena.impl.enclave;

import static org.junit.Assert.*;

import net.consensys.athena.impl.enclave.sodium.SodiumCombinedKey;
import net.consensys.athena.impl.enclave.sodium.SodiumPublicKey;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Serializer;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class SimpleEncryptedPayloadTest {

  @Test
  public void testRoundTripSerialization() {
    SodiumCombinedKey sodiumCombinedKey = new SodiumCombinedKey("Combined key fakery".getBytes());
    Map<PublicKey, Integer> combinedKeysOwners = new HashMap<>();
    //    PublicKey key = new SodiumPublicKey("fake remote publickey".getBytes());
    //    combinedKeysOwners.put(key, 1);
    SimpleEncryptedPayload payload =
        new SimpleEncryptedPayload(
            new SodiumPublicKey("fakekey".getBytes()),
            "fake nonce".getBytes(),
            "fake combinedNonce".getBytes(),
            new SodiumCombinedKey[] {sodiumCombinedKey},
            "fake ciphertext".getBytes(),
            combinedKeysOwners);
    Serializer serializer = new Serializer();
    byte[] bytes = serializer.serialize(ContentType.JSON, payload);
    assertEquals(
        payload, serializer.deserialize(ContentType.JSON, SimpleEncryptedPayload.class, bytes));
    bytes = serializer.serialize(ContentType.CBOR, payload);
    assertEquals(
        payload, serializer.deserialize(ContentType.CBOR, SimpleEncryptedPayload.class, bytes));
  }
}
