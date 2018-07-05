/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package net.consensys.orion.impl.enclave.sodium;

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

class SodiumEncryptedPayloadTest {

  @Test
  void roundTripSerialization() {
    SodiumCombinedKey sodiumCombinedKey = new SodiumCombinedKey("Combined key fakery".getBytes(UTF_8));
    Map<SodiumPublicKey, Integer> combinedKeysOwners = new HashMap<>();
    SodiumPublicKey key = new SodiumPublicKey("fake remote publickey".getBytes(UTF_8));
    combinedKeysOwners.put(key, 1);
    SodiumEncryptedPayload payload = new SodiumEncryptedPayload(
        new SodiumPublicKey("fakekey".getBytes(UTF_8)),
        "fake nonce".getBytes(UTF_8),
        "fake combinedNonce".getBytes(UTF_8),
        new SodiumCombinedKey[] {sodiumCombinedKey},
        "fake ciphertext".getBytes(UTF_8),
        Optional.of(combinedKeysOwners));
    assertEquals(payload, Serializer.roundTrip(HttpContentType.JSON, SodiumEncryptedPayload.class, payload));
    assertEquals(payload, Serializer.roundTrip(HttpContentType.CBOR, SodiumEncryptedPayload.class, payload));
  }

  @Test
  void serializationToJsonWithoutCombinedKeyOwners() throws Exception {
    SodiumCombinedKey sodiumCombinedKey = new SodiumCombinedKey("Combined key fakery".getBytes(UTF_8));
    SodiumEncryptedPayload payload = new SodiumEncryptedPayload(
        new SodiumPublicKey("fakekey".getBytes(UTF_8)),
        "fake nonce".getBytes(UTF_8),
        "fake combinedNonce".getBytes(UTF_8),
        new SodiumCombinedKey[] {sodiumCombinedKey},
        "fake ciphertext".getBytes(UTF_8),
        Optional.empty());
    byte[] serialized = Serializer.serialize(HttpContentType.JSON, payload);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(serialized);
    jsonNode.fieldNames();
  }
}
