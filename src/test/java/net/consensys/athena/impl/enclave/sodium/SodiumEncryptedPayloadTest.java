package net.consensys.athena.impl.enclave.sodium;

import static org.junit.Assert.assertEquals;

import net.consensys.athena.impl.http.server.HttpContentType;
import net.consensys.athena.impl.utils.Serializer;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class SodiumEncryptedPayloadTest {

  @Test
  public void testRoundTripSerialization() {
    SodiumCombinedKey sodiumCombinedKey = new SodiumCombinedKey("Combined key fakery".getBytes());
    Map<PublicKey, Integer> combinedKeysOwners = new HashMap<>();
    //    PublicKey key = new SodiumPublicKey("fake remote publickey".bytes());
    //    combinedKeysOwners.put(key, 1);
    SodiumEncryptedPayload payload =
        new SodiumEncryptedPayload(
            new SodiumPublicKey("fakekey".getBytes()),
            "fake nonce".getBytes(),
            "fake combinedNonce".getBytes(),
            new SodiumCombinedKey[] {sodiumCombinedKey},
            "fake ciphertext".getBytes(),
            combinedKeysOwners);
    Serializer serializer = new Serializer();
    assertEquals(
        payload, serializer.roundTrip(HttpContentType.JSON, SodiumEncryptedPayload.class, payload));
    assertEquals(
        payload, serializer.roundTrip(HttpContentType.CBOR, SodiumEncryptedPayload.class, payload));
  }
}
