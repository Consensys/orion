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
package net.consensys.orion.acceptance.send;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import net.consensys.orion.acceptance.dsl.AcceptanceTestBase;
import net.consensys.orion.acceptance.dsl.OrionNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.tuweni.crypto.sodium.Box;
import org.junit.jupiter.api.Test;

public class MultiKeyOperationAcceptanceTest extends AcceptanceTestBase {

  private final byte[] payload = "This Is My Data".getBytes(StandardCharsets.UTF_8);

  @Test
  public void dataTransfersFromOneNodeToAnother() throws IOException {
    final OrionNode bootnode = orionFactory().create("node_1", 1);
    final OrionNode secondNode = orionFactory().create("node_2", 1, singletonList(bootnode));
    final Box.PublicKey recipientKey = secondNode.getPublicKey(0);

    waitForClientInterconnect(bootnode, secondNode);

    final String key = bootnode.sendDataLegacy(payload, bootnode.getPublicKey(0), recipientKey);

    final byte[] dataInReceivingNode = secondNode.extractDataItem(key, recipientKey);
    assertThat(dataInReceivingNode).isEqualTo(payload);
  }


  @Test
  public void multiKeyPerNodeResultsInValidDataTransfer() throws IOException {

    final OrionNode bootnode = orionFactory().create("node_1", 10);
    final OrionNode secondNode = orionFactory().create("node_2", 10, singletonList(bootnode));

    waitForClientInterconnect(bootnode, secondNode);

    final Box.PublicKey[] recipientKeys = new Box.PublicKey[20];
    int j = 0;
    for (int i = 0; i < 10; i++) {
      recipientKeys[j++] = bootnode.getPublicKey(i);
      recipientKeys[j++] = secondNode.getPublicKey(i);
    }

    final String key = bootnode.sendDataLegacy(payload, bootnode.getPublicKey(1), recipientKeys);

    byte[] dataInReceivingNode = null;
    j = 0;
    for (int i = 0; i < 10; i++) {
      dataInReceivingNode = bootnode.extractDataItem(key, recipientKeys[j++]);
      assertThat(dataInReceivingNode).isEqualTo(payload);
      dataInReceivingNode = secondNode.extractDataItem(key, recipientKeys[j++]);
      assertThat(dataInReceivingNode).isEqualTo(payload);
    }
  }

  @Test
  public void multiKeyOneNodeResultsInValidDataTransfer() throws IOException {

    final OrionNode bootnode = orionFactory().create("node_1", 10);
    final Box.PublicKey recipientKey1 = bootnode.getPublicKey(1);
    final Box.PublicKey recipientKey2 = bootnode.getPublicKey(2);
    final Box.PublicKey recipientKey3 = bootnode.getPublicKey(3);
    final Box.PublicKey recipientKey4 = bootnode.getPublicKey(4);

    final String key =
        bootnode.sendDataLegacy(payload, recipientKey1, recipientKey1, recipientKey2, recipientKey3, recipientKey4);

    byte[] dataInReceivingNode = bootnode.extractDataItem(key, recipientKey1);
    assertThat(dataInReceivingNode).isEqualTo(payload);
    dataInReceivingNode = bootnode.extractDataItem(key, recipientKey2);
    assertThat(dataInReceivingNode).isEqualTo(payload);
    dataInReceivingNode = bootnode.extractDataItem(key, recipientKey3);
    assertThat(dataInReceivingNode).isEqualTo(payload);
    dataInReceivingNode = bootnode.extractDataItem(key, recipientKey4);
    assertThat(dataInReceivingNode).isEqualTo(payload);
  }

  @Test
  public void multiKeyOneNodeCreatePrivacyGroupResultsInValidDataTransfer() throws IOException {

    final OrionNode bootnode = orionFactory().create("node_1", 10);
    final Box.PublicKey recipientKey1 = bootnode.getPublicKey(1);
    final Box.PublicKey recipientKey2 = bootnode.getPublicKey(2);
    final Box.PublicKey recipientKey3 = bootnode.getPublicKey(3);

    final String pgid = bootnode.createPrivacyGroup(recipientKey1, "", "", recipientKey1, recipientKey2, recipientKey3);

    final String key = bootnode.sendDataPrivacyGroup(payload, recipientKey1, pgid);

    byte[] dataInReceivingNode = bootnode.extractDataItem(key, recipientKey1);
    assertThat(dataInReceivingNode).isEqualTo(payload);
    dataInReceivingNode = bootnode.extractDataItem(key, recipientKey2);
    assertThat(dataInReceivingNode).isEqualTo(payload);
    dataInReceivingNode = bootnode.extractDataItem(key, recipientKey3);
    assertThat(dataInReceivingNode).isEqualTo(payload);
  }

  @Test
  public void multiKeyTwoNodesResultsInValidDataTransfer() throws IOException {

    final OrionNode bootnode = orionFactory().create("node_1", 10);
    final OrionNode secondNode = orionFactory().create("node_2", 10, singletonList(bootnode));

    waitForClientInterconnect(bootnode, secondNode);

    final Box.PublicKey recipientKey1 = bootnode.getPublicKey(1);
    final Box.PublicKey recipientKey2 = bootnode.getPublicKey(2);
    final Box.PublicKey recipientKey3 = secondNode.getPublicKey(3);
    final Box.PublicKey recipientKey4 = secondNode.getPublicKey(4);

    final String key =
        bootnode.sendDataLegacy(payload, recipientKey1, recipientKey1, recipientKey2, recipientKey3, recipientKey4);

    byte[] dataInReceivingNode = bootnode.extractDataItem(key, recipientKey1);
    assertThat(dataInReceivingNode).isEqualTo(payload);
    dataInReceivingNode = bootnode.extractDataItem(key, recipientKey2);
    assertThat(dataInReceivingNode).isEqualTo(payload);
    dataInReceivingNode = secondNode.extractDataItem(key, recipientKey3);
    assertThat(dataInReceivingNode).isEqualTo(payload);
    dataInReceivingNode = secondNode.extractDataItem(key, recipientKey4);
    assertThat(dataInReceivingNode).isEqualTo(payload);
  }

  @Test
  public void multiKeyTwoNodesCreatePrivacyGroupResultsInValidDataTransfer() throws IOException {

    final OrionNode bootnode = orionFactory().create("node_1", 10);
    final OrionNode secondNode = orionFactory().create("node_2", 10, singletonList(bootnode));

    waitForClientInterconnect(bootnode, secondNode);

    final Box.PublicKey recipientKey1 = bootnode.getPublicKey(1);
    final Box.PublicKey recipientKey2 = bootnode.getPublicKey(2);
    final Box.PublicKey recipientKey3 = secondNode.getPublicKey(3);
    final Box.PublicKey recipientKey4 = secondNode.getPublicKey(4);

    final String pgid =
        bootnode.createPrivacyGroup(recipientKey1, "", "", recipientKey1, recipientKey2, recipientKey3, recipientKey4);

    final String key = bootnode.sendDataPrivacyGroup(payload, recipientKey1, pgid);

    byte[] dataInReceivingNode = bootnode.extractDataItem(key, recipientKey1);
    assertThat(dataInReceivingNode).isEqualTo(payload);
    dataInReceivingNode = bootnode.extractDataItem(key, recipientKey2);
    assertThat(dataInReceivingNode).isEqualTo(payload);
    dataInReceivingNode = secondNode.extractDataItem(key, recipientKey3);
    assertThat(dataInReceivingNode).isEqualTo(payload);
    dataInReceivingNode = secondNode.extractDataItem(key, recipientKey4);
    assertThat(dataInReceivingNode).isEqualTo(payload);
  }
}
