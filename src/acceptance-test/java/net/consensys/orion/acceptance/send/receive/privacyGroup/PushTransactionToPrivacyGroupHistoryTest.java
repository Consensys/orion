/*
 * Copyright 2019 ConsenSys AG.
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
package net.consensys.orion.acceptance.send.receive.privacyGroup;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.orion.acceptance.NodeUtils.createPrivacyGroupTransaction;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.acceptance.EthClientStub;
import net.consensys.orion.acceptance.NodeUtils;
import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.enclave.sodium.MemoryKeyStore;
import net.consensys.orion.enclave.sodium.SodiumEnclave;
import net.consensys.orion.http.handler.privacy.PrivacyGroup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class PushTransactionToPrivacyGroupHistoryTest extends PrivacyGroupAcceptanceTestBase {

  private MemoryKeyStore memoryKeyStore;

  @BeforeEach
  void setUpKeystore() throws Exception {

    memoryKeyStore = new MemoryKeyStore();
  }

  @Test
  void receiverCanViewWhenSentToPrivacyGroup() {
    final EthClientStub firstClient = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.nodePort(), firstHttpClient);

    String[] addresses = new String[] {PK_1_B_64, PK_2_B_64};
    final PrivacyGroup privacyGroup =
        createPrivacyGroupTransaction(firstClient, addresses, PK_1_B_64, "testName", "testDescription");

    EncryptedPayload payload = mockPayload();
    var pushResult = firstNode.push(payload).orElseThrow();
    var result = firstClient.pushToHistory(privacyGroup.getPrivacyGroupId(), "0xnotahash", pushResult);
    assertTrue(result.isPresent() && result.get());
  }

  @Test
  void receiverCanViewWhenSentToLargePrivacyGroup() {
    final EthClientStub firstClient = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.nodePort(), firstHttpClient);

    final EthClientStub secondClient = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);
    final EthClientStub secondNode = NodeUtils.client(firstOrionLauncher.nodePort(), firstHttpClient);

    String[] addresses = new String[] {PK_1_B_64, PK_2_B_64, PK_3_B_64};
    final PrivacyGroup privacyGroup =
        createPrivacyGroupTransaction(firstClient, addresses, PK_1_B_64, "testName", "testDescription");

    EncryptedPayload payload = mockPayload();
    var pushResult = firstNode.push(payload).orElseThrow();
    var secondPushResult = secondNode.push(payload).orElseThrow();
    var result = firstClient.pushToHistory(privacyGroup.getPrivacyGroupId(), "0xnotahash", pushResult);
    var secondResult = secondClient.pushToHistory(privacyGroup.getPrivacyGroupId(), "0xnotahash", secondPushResult);
    assertTrue(result.isPresent() && result.get());
    assertTrue(secondResult.isPresent() && secondResult.get());
  }


  @Test
  void errorWhenIncorrectHistoryPushedToPrivacyGroup() {
    final EthClientStub firstClient = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.nodePort(), firstHttpClient);

    String[] addresses = new String[] {PK_1_B_64, PK_2_B_64};
    final PrivacyGroup privacyGroup =
        createPrivacyGroupTransaction(firstClient, addresses, PK_1_B_64, "testName", "testDescription");

    EncryptedPayload payload = mockPayload();
    firstNode.push(payload).orElseThrow();

    var pushResult = firstClient.pushToHistory(privacyGroup.getPrivacyGroupId(), "0xnotahash", "invalid payload");
    assertFalse(pushResult.isPresent());
  }

  private EncryptedPayload mockPayload() {
    SodiumEnclave sEnclave = new SodiumEnclave(memoryKeyStore);
    Box.PublicKey k1 = memoryKeyStore.generateKeyPair();
    Box.PublicKey k2 = memoryKeyStore.generateKeyPair();
    return sEnclave.encrypt("something important".getBytes(UTF_8), k1, new Box.PublicKey[] {k2}, null);
  }
}
