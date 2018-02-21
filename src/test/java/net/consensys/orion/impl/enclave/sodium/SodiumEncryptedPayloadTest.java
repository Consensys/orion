package net.consensys.orion.impl.enclave.sodium;

import static org.junit.Assert.assertEquals;

import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Serializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class SodiumEncryptedPayloadTest {

  @Test
  public void testRoundTripSerialization() {
    SodiumCombinedKey sodiumCombinedKey = new SodiumCombinedKey("Combined key fakery".getBytes());
    Map<SodiumPublicKey, Integer> combinedKeysOwners = new HashMap<>();
    SodiumPublicKey key = new SodiumPublicKey("fake remote publickey".getBytes());
    combinedKeysOwners.put(key, 1);
    SodiumEncryptedPayload payload =
        new SodiumEncryptedPayload(
            new SodiumPublicKey("fakekey".getBytes()),
            "fake nonce".getBytes(),
            "fake combinedNonce".getBytes(),
            new SodiumCombinedKey[] {sodiumCombinedKey},
            "fake ciphertext".getBytes(),
            Optional.of(combinedKeysOwners));
    Serializer serializer = new Serializer();
    assertEquals(
        payload, serializer.roundTrip(HttpContentType.JSON, SodiumEncryptedPayload.class, payload));
    assertEquals(
        payload, serializer.roundTrip(HttpContentType.CBOR, SodiumEncryptedPayload.class, payload));
  }

  @Test
  public void testSerializationToJsonWithoutCombinedKeyOwners() throws Exception {
    SodiumCombinedKey sodiumCombinedKey = new SodiumCombinedKey("Combined key fakery".getBytes());
    SodiumEncryptedPayload payload =
        new SodiumEncryptedPayload(
            new SodiumPublicKey("fakekey".getBytes()),
            "fake nonce".getBytes(),
            "fake combinedNonce".getBytes(),
            new SodiumCombinedKey[] {sodiumCombinedKey},
            "fake ciphertext".getBytes(),
            Optional.empty());
    Serializer serializer = new Serializer();
    byte[] serialized = serializer.serialize(HttpContentType.JSON, payload);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(serialized);
    jsonNode.fieldNames();
  }
}
