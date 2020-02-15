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
package net.consensys.orion.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.orion.enclave.Enclave;
import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.enclave.sodium.SodiumEnclaveStub;

import java.security.Security;
import java.util.Optional;
import java.util.Random;

import org.apache.tuweni.kv.MapKeyValueStore;
import org.junit.jupiter.api.Test;

class EncryptedPayloadStorageTest {

  static {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  private final Enclave enclave = new SodiumEnclaveStub();
  private final StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder();
  private final Storage<EncryptedPayload> payloadStorage =
      new EncryptedPayloadStorage(new MapKeyValueStore<>(), keyBuilder);

  @Test
  void storeAndRetrieve() throws Exception {
    // generate random byte content
    final byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    final EncryptedPayload toStore = enclave.encrypt(toEncrypt, null, null, null);

    final String key = payloadStorage.put(toStore).get();
    assertEquals(toStore, payloadStorage.get(key).get().get());
  }

  @Test
  void retrieveWithoutStore() throws Exception {
    assertEquals(Optional.empty(), payloadStorage.get("missing").get());
  }
}
