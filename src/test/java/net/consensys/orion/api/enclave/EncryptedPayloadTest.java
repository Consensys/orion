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

package net.consensys.orion.api.enclave;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Serializer;

import java.util.HashMap;
import java.util.Map;

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
        combinedKeysOwners);
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
        "fake ciphertext".getBytes(UTF_8));
    byte[] serialized = Serializer.serialize(HttpContentType.JSON, payload);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(serialized);
    jsonNode.fieldNames();
  }
}
