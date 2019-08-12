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
package net.consensys.orion.acceptance.dsl;


import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import org.awaitility.Awaitility;

public class AcceptanceTestBase {

  private static final OrionFactory orionFactory = new OrionFactory();

  public OrionFactory orionFactory() {
    return orionFactory;
  }

  public void waitForClientInterconnect(final int timeoutSeconds, final OrionNode... nodes) {
    List<OrionNode> nodeList = Lists.newArrayList(nodes);

    // The value reported by PeerCount _appears_ to be calculated by adding:
    // 1. Yourself
    // 2. The number of keys reported by remote peers
    // 3. The number of bootnodes specified

    final int expectedKeyEndpoints = nodeList.stream().map(OrionNode::getPublicKeyCount).reduce(0, Integer::sum);

    for (final OrionNode node : nodeList) {
      //
      final int expectedPeerCount = expectedKeyEndpoints - node.getPublicKeyCount() + 1 + node.getBootnodeCount();
      Awaitility.waitAtMost(timeoutSeconds, TimeUnit.SECONDS).until(() -> node.peerCount() == expectedPeerCount);
    }
  }

  public void waitForClientInterconnect(final OrionNode... nodes) {
    waitForClientInterconnect(5, nodes);
  }
}
