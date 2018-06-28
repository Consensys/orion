package net.consensys.orion.api.enclave;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Serializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class EncryptedPayloadTest {

  @Test
  void roundTripSerialization() {
    CombinedKey combinedKey = new CombinedKey("Combined key fakery".getBytes(UTF_8));
    Map<PublicKey, Integer> combinedKeysOwners = new HashMap<>();
    PublicKey key = new PublicKey("fake remote publickey".getBytes(UTF_8));
    combinedKeysOwners.put(key, 1);
    EncryptedPayload payload = new EncryptedPayload(
        new PublicKey("fakekey".getBytes(UTF_8)),
        "fake nonce".getBytes(UTF_8),
        "fake combinedNonce".getBytes(UTF_8),
        new CombinedKey[] {combinedKey},
        "fake ciphertext".getBytes(UTF_8),
        Optional.of(combinedKeysOwners));
    assertEquals(payload, Serializer.roundTrip(HttpContentType.JSON, EncryptedPayload.class, payload));
    assertEquals(payload, Serializer.roundTrip(HttpContentType.CBOR, EncryptedPayload.class, payload));
  }

  @Test
  void serializationToJsonWithoutCombinedKeyOwners() throws Exception {
    CombinedKey combinedKey = new CombinedKey("Combined key fakery".getBytes(UTF_8));
    EncryptedPayload payload = new EncryptedPayload(
        new PublicKey("fakekey".getBytes(UTF_8)),
        "fake nonce".getBytes(UTF_8),
        "fake combinedNonce".getBytes(UTF_8),
        new CombinedKey[] {combinedKey},
        "fake ciphertext".getBytes(UTF_8),
        Optional.empty());
    byte[] serialized = Serializer.serialize(HttpContentType.JSON, payload);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(serialized);
    jsonNode.fieldNames();
  }
}
