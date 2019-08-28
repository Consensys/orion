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

import static net.consensys.orion.acceptance.NodeUtils.createPrivacyGroupTransaction;
import static net.consensys.orion.acceptance.NodeUtils.findPrivacyGroupTransaction;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.acceptance.EthClientStub;
import net.consensys.orion.acceptance.NodeUtils;
import net.consensys.orion.acceptance.WaitUtils;
import net.consensys.orion.http.handler.privacy.PrivacyGroup;

import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Runs up a three nodes that communicates with each other and tries to add the third node to an existing privacy group.
 */
@ExtendWith(TempDirectoryExtension.class)
class AddToPrivacyGroupTest extends PrivacyGroupAcceptanceTest {

  @Test
  void addToPrivacyGroup() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);
    final EthClientStub secondNode = NodeUtils.client(secondOrionLauncher.clientPort(), secondHttpClient);
    final EthClientStub thirdNode = NodeUtils.client(thirdOrionLauncher.clientPort(), thirdHttpClient);

    final String name = "testName";
    final String description = "testDescription";
    final String[] addresses = new String[] {PK_1_B_64, PK_2_B_64};
    // create a privacy group
    final PrivacyGroup privacyGroup = createPrivacyGroupTransaction(firstNode, addresses, PK_1_B_64, name, description);

    final String privacyGroupId = privacyGroup.getPrivacyGroupId();
    assertEquals(name, privacyGroup.getName());
    assertEquals(description, privacyGroup.getDescription());
    assertEquals(2, privacyGroup.getMembers().length);

    NodeUtils.addToPrivacyGroup(firstNode, PK_3_B_64, PK_1_B_64, privacyGroupId);

    final String[] newGroupMembers = new String[] {PK_1_B_64, PK_2_B_64, PK_3_B_64};

    WaitUtils.waitFor(() -> assertEquals(1, findPrivacyGroupTransaction(secondNode, newGroupMembers).length));

    final PrivacyGroup[] propagatedToExistingNode = findPrivacyGroupTransaction(secondNode, newGroupMembers);
    assertArrayEquals(newGroupMembers, propagatedToExistingNode[0].getMembers());
    assertEquals(privacyGroupId, propagatedToExistingNode[0].getPrivacyGroupId());

    final PrivacyGroup[] propagatedToOriginalNode = findPrivacyGroupTransaction(firstNode, newGroupMembers);
    assertArrayEquals(newGroupMembers, propagatedToOriginalNode[0].getMembers());
    assertEquals(privacyGroupId, propagatedToOriginalNode[0].getPrivacyGroupId());

    final PrivacyGroup[] propagatedToNewNode = findPrivacyGroupTransaction(thirdNode, newGroupMembers);
    assertArrayEquals(newGroupMembers, propagatedToNewNode[0].getMembers());
    assertEquals(privacyGroupId, propagatedToNewNode[0].getPrivacyGroupId());
  }

  @Test
  void addToPrivacyGroupFromInvalidNode() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);
    final EthClientStub thirdNode = NodeUtils.client(thirdOrionLauncher.clientPort(), thirdHttpClient);

    final String name = "testName";
    final String description = "testDescription";
    final String[] addresses = new String[] {PK_1_B_64, PK_2_B_64};

    final PrivacyGroup privacyGroup = createPrivacyGroupTransaction(firstNode, addresses, PK_1_B_64, name, description);

    assertThrows(
        AssertionFailedError.class,
        () -> NodeUtils.addToPrivacyGroup(thirdNode, PK_3_B_64, PK_1_B_64, privacyGroup.getPrivacyGroupId()));
  }

  @Test
  void addToPrivacyGroupMissingParams() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);

    final String name = "testName";
    final String description = "testDescription";
    final String[] addresses = new String[] {PK_1_B_64, PK_2_B_64};
    // create a privacy group
    final PrivacyGroup privacyGroup = createPrivacyGroupTransaction(firstNode, addresses, PK_1_B_64, name, description);

    final String privacyGroupId = privacyGroup.getPrivacyGroupId();

    assertThrows(
        AssertionFailedError.class,
        () -> NodeUtils.addToPrivacyGroup(firstNode, "not valid", PK_1_B_64, privacyGroupId));
  }
}
