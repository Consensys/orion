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

import static net.consensys.cava.io.Base64.encodeBytes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.cava.kv.KeyValueStore;
import net.consensys.orion.enclave.Enclave;
import net.consensys.orion.enclave.PrivacyGroupPayload;
import net.consensys.orion.enclave.sodium.MemoryKeyStore;
import net.consensys.orion.enclave.sodium.SodiumEnclave;

import java.nio.file.Path;
import java.security.Security;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class PrivacyGroupStorageTest {
  private MemoryKeyStore memoryKeyStore;
  private KeyValueStore storage;

  static {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  private Enclave enclave;
  private Storage<PrivacyGroupPayload> payloadStorage;

  @BeforeEach
  void setup(@TempDirectory Path tempDir) throws SQLException {
    memoryKeyStore = new MemoryKeyStore();
    Box.PublicKey defaultNodeKey = memoryKeyStore.generateKeyPair();
    memoryKeyStore.addNodeKey(defaultNodeKey);
    enclave = new SodiumEnclave(memoryKeyStore);

    String jdbcUrl = "jdbc:h2:" + tempDir.resolve("node2").toString();
    try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
      Statement st = conn.createStatement();
      st.executeUpdate("create table if not exists store(key char(60), value binary, primary key(key))");
    }
    final JpaEntityManagerProvider jpaEntityManagerProvider = new JpaEntityManagerProvider(jdbcUrl);
    storage = new OrionSQLKeyValueStore(jpaEntityManagerProvider);
    payloadStorage = new PrivacyGroupStorage(storage, enclave);
  }

  @Test
  void putAndGet() throws InterruptedException {
    Box.PublicKey senderKey = memoryKeyStore.generateKeyPair();
    Box.PublicKey recipientKey = memoryKeyStore.generateKeyPair();

    String[] toEncrypt = new String[] {encodeBytes(senderKey.bytesArray()), encodeBytes(recipientKey.bytesArray())};

    // generate random byte content
    byte[] seed = new byte[20];
    new Random().nextBytes(seed);

    PrivacyGroupPayload privacyGroupPayload1 = new PrivacyGroupPayload(
        toEncrypt,
        "name",
        "des",
        PrivacyGroupPayload.State.ACTIVE,
        PrivacyGroupPayload.Type.PANTHEON,
        seed);

    String key1 = payloadStorage.put(privacyGroupPayload1).get();
    Optional<PrivacyGroupPayload> optionalPrivacyGroupPayload = payloadStorage.get(key1).get();
    assertNotNull(optionalPrivacyGroupPayload);
    assertTrue(optionalPrivacyGroupPayload.isPresent());
    assertEquals(optionalPrivacyGroupPayload.get().state(), PrivacyGroupPayload.State.ACTIVE);
  }

  @Test
  void storeTwice() throws InterruptedException {
    Box.PublicKey senderKey = memoryKeyStore.generateKeyPair();
    Box.PublicKey recipientKey = memoryKeyStore.generateKeyPair();

    String[] toEncrypt = new String[] {encodeBytes(senderKey.bytesArray()), encodeBytes(recipientKey.bytesArray())};

    // generate random byte content
    byte[] seed = new byte[20];
    new Random().nextBytes(seed);

    PrivacyGroupPayload privacyGroupPayload1 = new PrivacyGroupPayload(
        toEncrypt,
        "name",
        "des",
        PrivacyGroupPayload.State.ACTIVE,
        PrivacyGroupPayload.Type.PANTHEON,
        seed);

    String key1 = payloadStorage.put(privacyGroupPayload1).get();

    PrivacyGroupPayload privacyGroupPayload2 = new PrivacyGroupPayload(
        toEncrypt,
        "name",
        "des",
        PrivacyGroupPayload.State.DELETED,
        PrivacyGroupPayload.Type.PANTHEON,
        seed);

    String key2 = payloadStorage.put(privacyGroupPayload2).get();
    assertEquals(key1, key2);
  }
}
