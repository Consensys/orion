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
package net.consensys.orion.enclave;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.orion.enclave.sodium.MemoryKeyStore;
import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.utils.Serializer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class EncryptedPayloadTest {

  @Test
  void roundTripSerialization() {
    final EncryptedKey encryptedKey = new EncryptedKey("Encrypted key fakery".getBytes(UTF_8));
    final Map<Box.PublicKey, Integer> encryptedKeysOwners = new HashMap<>();
    final Box.PublicKey key = Box.KeyPair.random().publicKey();
    encryptedKeysOwners.put(key, 1);
    final EncryptedPayload payload = new EncryptedPayload(
        Box.KeyPair.random().publicKey(),
        "fake nonce".getBytes(UTF_8),
        new EncryptedKey[] {encryptedKey},
        "fake ciphertext".getBytes(UTF_8),
        encryptedKeysOwners,
        "fake group".getBytes(UTF_8));
    assertEquals(payload, Serializer.roundTrip(HttpContentType.JSON, EncryptedPayload.class, payload));
    assertEquals(payload, Serializer.roundTrip(HttpContentType.CBOR, EncryptedPayload.class, payload));
  }

  @Test
  void serializationToJsonWithoutEncryptedKeyOwners() throws Exception {
    final EncryptedKey encryptedKey = new EncryptedKey("Encrypted key fakery".getBytes(UTF_8));
    final EncryptedPayload payload = new EncryptedPayload(
        Box.KeyPair.random().publicKey(),
        "fake nonce".getBytes(UTF_8),
        new EncryptedKey[] {encryptedKey},
        "fake ciphertext".getBytes(UTF_8),
        "fake group".getBytes(UTF_8));
    final byte[] serialized = Serializer.serialize(HttpContentType.JSON, payload);
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode jsonNode = mapper.readTree(serialized);
    jsonNode.fieldNames();
  }

  @Test
  void stripKeysStrippingCorrectly() throws Exception {
    final EncryptedKey encryptedKey1 = new EncryptedKey("Encrypted key1".getBytes(UTF_8));
    final EncryptedKey encryptedKey2 = new EncryptedKey("Encrypted key2".getBytes(UTF_8));
    final EncryptedKey encryptedKey3 = new EncryptedKey("Encrypted key3".getBytes(UTF_8));
    final MemoryKeyStore keyStore = new MemoryKeyStore();
    final Box.PublicKey publicKey1 = keyStore.generateKeyPair();
    final Box.PublicKey publicKey2 = keyStore.generateKeyPair();
    final Box.PublicKey publicKey3 = keyStore.generateKeyPair();
    final Map<Box.PublicKey, Integer> keyMap = new HashMap<>();
    keyMap.put(publicKey1, 0);
    keyMap.put(publicKey2, 1);
    keyMap.put(publicKey3, 2);

    final EncryptedPayload payload = new EncryptedPayload(
        Box.KeyPair.random().publicKey(),
        "fake nonce".getBytes(UTF_8),
        new EncryptedKey[] {encryptedKey1, encryptedKey2, encryptedKey3},
        "fake ciphertext".getBytes(UTF_8),
        keyMap,
        "fake group".getBytes(UTF_8));

    final EncryptedPayload strippedPayload = payload.stripFor(Arrays.asList(publicKey1, publicKey3));

    assertTrue(strippedPayload.encryptedKeys().length == 2);
    final List<String> strings =
        Arrays.asList(strippedPayload.encryptedKeys()).stream().map(e -> new String(e.getEncoded(), UTF_8)).collect(
            Collectors.toList());
    assertTrue(strings.containsAll(Arrays.asList("Encrypted key1", "Encrypted key3")));
  }
}
