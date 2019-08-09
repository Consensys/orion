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
package net.consensys.orion.acceptance;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import net.consensys.orion.acceptance.dsl.AcceptanceTestBase;
import net.consensys.orion.acceptance.dsl.OrionNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.Test;

public class MultiKeyOperationAcceptanceTest extends AcceptanceTestBase {
  private final byte[] payload = "This Is My Data".getBytes(StandardCharsets.UTF_8);

  @Test
  public void dataTransfersFromOneNodeToAnother() throws IOException {
    final OrionNode bootnode = orionFactory().create("node_1", 1);
    final OrionNode secondNode = orionFactory().create("node_2", 1, singletonList(bootnode));

    waitForClientInterconnect(bootnode, secondNode);

    final String key = bootnode.sendData(payload, bootnode.getPublicKey(0), singletonList(secondNode.getPublicKey(0)));

    final byte[] dataInReceivingNode = secondNode.extractDataItem(key, secondNode.getPublicKey(0));
    assertThat(dataInReceivingNode).isEqualTo(payload);
  }


  @Test
  public void multiKeyPerNodeResultsInValidDataTransfer() throws IOException {
    final OrionNode bootnode = orionFactory().create("node_1", 2);
    final OrionNode secondNode = orionFactory().create("node_2", 2, singletonList(bootnode));

    waitForClientInterconnect(bootnode, secondNode);

    final String key = bootnode.sendData(payload, bootnode.getPublicKey(1), singletonList(secondNode.getPublicKey(1)));

    final byte[] dataInReceivingNode = secondNode.extractDataItem(key, secondNode.getPublicKey(1));
    assertThat(dataInReceivingNode).isEqualTo(payload);
  }
}
