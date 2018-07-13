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

import net.consensys.cava.crypto.sodium.Box;
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
    EncryptedKey encryptedKey = new EncryptedKey("Encrypted key fakery".getBytes(UTF_8));
    Map<Box.PublicKey, Integer> encryptedKeysOwners = new HashMap<>();
    Box.PublicKey key = Box.KeyPair.random().publicKey();
    encryptedKeysOwners.put(key, 1);
    EncryptedPayload payload = new EncryptedPayload(
        Box.KeyPair.random().publicKey(),
        "fake nonce".getBytes(UTF_8),
        new EncryptedKey[] {encryptedKey},
        "fake ciphertext".getBytes(UTF_8),
        encryptedKeysOwners);
    assertEquals(payload, Serializer.roundTrip(HttpContentType.JSON, EncryptedPayload.class, payload));
    assertEquals(payload, Serializer.roundTrip(HttpContentType.CBOR, EncryptedPayload.class, payload));
  }

  @Test
  void serializationToJsonWithoutEncryptedKeyOwners() throws Exception {
    EncryptedKey encryptedKey = new EncryptedKey("Encrypted key fakery".getBytes(UTF_8));
    EncryptedPayload payload = new EncryptedPayload(
        Box.KeyPair.random().publicKey(),
        "fake nonce".getBytes(UTF_8),
        new EncryptedKey[] {encryptedKey},
        "fake ciphertext".getBytes(UTF_8));
    byte[] serialized = Serializer.serialize(HttpContentType.JSON, payload);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(serialized);
    jsonNode.fieldNames();
  }
}
