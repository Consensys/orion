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
    final List<OrionNode> nodeList = Lists.newArrayList(nodes);

    final int expectedKeyEndpoints = nodeList.size();

    for (final OrionNode node : nodeList) {
      Awaitility.waitAtMost(timeoutSeconds, TimeUnit.SECONDS).until(() -> {
        System.out.println("COUNT: " + node.peerCount() + "   EXPECTED: " + expectedKeyEndpoints);
        return node.peerCount() == expectedKeyEndpoints;
      });
    }
  }

  public void waitForClientInterconnect(final OrionNode... nodes) {
    waitForClientInterconnect(30, nodes);
  }
}
