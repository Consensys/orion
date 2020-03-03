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

import static io.vertx.core.Vertx.vertx;
import static net.consensys.orion.acceptance.NodeUtils.createPrivacyGroup;
import static net.consensys.orion.acceptance.NodeUtils.deletePrivacyGroup;
import static net.consensys.orion.acceptance.NodeUtils.findPrivacyGroup;
import static net.consensys.orion.acceptance.NodeUtils.joinPathsAsTomlListEntry;
import static net.consensys.orion.acceptance.NodeUtils.retrievePrivacyGroupTransaction;
import static net.consensys.orion.acceptance.NodeUtils.sendTransaction;
import static net.consensys.orion.http.server.HttpContentType.CBOR;
import static org.apache.tuweni.io.Base64.decodeBytes;
import static org.apache.tuweni.io.file.Files.copyResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.consensys.orion.acceptance.EthClientStub;
import net.consensys.orion.acceptance.NodeUtils;
import net.consensys.orion.cmd.Orion;
import net.consensys.orion.config.Config;
import net.consensys.orion.http.handler.privacy.PrivacyGroup;
import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.network.PersistentNetworkNodes;
import net.consensys.orion.network.ReadOnlyNetworkNodes;
import net.consensys.orion.utils.Serializer;

import java.net.URI;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import junit.framework.AssertionFailedError;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.crypto.sodium.Box;
import org.apache.tuweni.junit.TempDirectoryExtension;
import org.apache.tuweni.kv.MapKeyValueStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/** Runs up a two nodes that communicates with each other. */
@ExtendWith(TempDirectoryExtension.class)
class DualNodesPrivacyGroupsTest {

  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String PK_2_B_64 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";

  private Orion firstOrionLauncher;
  private Orion secondOrionLauncher;
  private Vertx vertx;
  private HttpClient firstHttpClient;
  private HttpClient secondHttpClient;

  @BeforeEach
  void setUpDualNodes(@TempDir final Path tempDir) throws Exception {

    final Path key1pub = copyResource("key1.pub", tempDir.resolve("key1.pub"));
    final Path key1key = copyResource("key1.key", tempDir.resolve("key1.key"));
    final Path key2pub = copyResource("key2.pub", tempDir.resolve("key2.pub"));
    final Path key2key = copyResource("key2.key", tempDir.resolve("key2.key"));
    final String jdbcUrl = "jdbc:h2:" + tempDir.resolve("DualNodesPrivacyGroupsTest").toString();
    try (final Connection conn = DriverManager.getConnection(jdbcUrl)) {
      final Statement st = conn.createStatement();
      st.executeUpdate("create table if not exists store(key char(60), value binary, primary key(key))");
    }

    final Config firstNodeConfig = NodeUtils.nodeConfig(
        tempDir,
        0,
        "127.0.0.1",
        0,
        "127.0.0.1",
        "node1",
        joinPathsAsTomlListEntry(key1pub),
        joinPathsAsTomlListEntry(key1key),
        "off",
        "tofu",
        "tofu",
        "leveldb:" + tempDir + "database/DualNodesPrivacyGroupsTest",
        "memory");
    final Config secondNodeConfig = NodeUtils.nodeConfig(
        tempDir,
        0,
        "127.0.0.1",
        0,
        "127.0.0.1",
        "node2",
        joinPathsAsTomlListEntry(key2pub),
        joinPathsAsTomlListEntry(key2key),
        "off",
        "tofu",
        "tofu",
        "sql:" + jdbcUrl,
        "memory");

    vertx = vertx();
    firstOrionLauncher = NodeUtils.startOrion(firstNodeConfig);
    firstHttpClient = vertx.createHttpClient();
    secondOrionLauncher = NodeUtils.startOrion(secondNodeConfig);
    secondHttpClient = vertx.createHttpClient();
    final Box.PublicKey pk1 = Box.PublicKey.fromBytes(decodeBytes(PK_1_B_64));
    final Box.PublicKey pk2 = Box.PublicKey.fromBytes(decodeBytes(PK_2_B_64));
    final PersistentNetworkNodes networkNodes =
        new PersistentNetworkNodes(firstNodeConfig, new Box.PublicKey[] {}, MapKeyValueStore.open());
    networkNodes.setNodeUrl(NodeUtils.uri("127.0.0.1", firstOrionLauncher.nodePort()), new Box.PublicKey[0]);
    Map<Bytes, URI> pks = new HashMap<>();
    pks.put(pk1.bytes(), NodeUtils.uri("127.0.0.1", firstOrionLauncher.nodePort()));
    pks.put(pk2.bytes(), NodeUtils.uri("127.0.0.1", secondOrionLauncher.nodePort()));

    networkNodes.addNode(pks.entrySet());
    // prepare /partyinfo payload (our known peers)
    final RequestBody partyInfoBody =
        RequestBody.create(MediaType.parse(CBOR.httpHeaderValue), Serializer.serialize(CBOR, networkNodes));
    // call http endpoint
    final OkHttpClient httpClient = new OkHttpClient();

    final String firstNodeBaseUrl = NodeUtils.urlString("127.0.0.1", firstOrionLauncher.nodePort());
    final Request request = new Request.Builder().post(partyInfoBody).url(firstNodeBaseUrl + "/partyinfo").build();
    // first /partyinfo call may just get the one node, so wait until we get at least 2 nodes
    await().atMost(10, TimeUnit.SECONDS).until(() -> getPartyInfoResponse(httpClient, request).nodeURIs().size() == 2);

  }

  private ReadOnlyNetworkNodes getPartyInfoResponse(final OkHttpClient httpClient, final Request request)
      throws Exception {
    final Response resp = httpClient.newCall(request).execute();
    assertEquals(200, resp.code());

    return Serializer.deserialize(HttpContentType.CBOR, ReadOnlyNetworkNodes.class, resp.body().bytes());
  }

  @AfterEach
  void tearDown() {
    await().atMost(5, TimeUnit.SECONDS).until(() -> doesNotThrowWhenCallingStop(firstOrionLauncher));
    await().atMost(5, TimeUnit.SECONDS).until(() -> doesNotThrowWhenCallingStop(secondOrionLauncher));
    vertx.close();
  }

  private Boolean doesNotThrowWhenCallingStop(final Orion orionLauncher) {
    try {
      orionLauncher.stop();
      return true;
    } catch (final Exception e) {
      return false;
    }
  }

  @Test
  void createAndFind() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);
    final EthClientStub secondNode = NodeUtils.client(secondOrionLauncher.clientPort(), secondHttpClient);

    final String name = "testName";
    final String description = "testDescription";
    final String[] addresses = new String[] {PK_1_B_64, PK_2_B_64};
    // create a privacy group
    final PrivacyGroup privacyGroup = createPrivacyGroup(firstNode, addresses, PK_1_B_64, name, description);

    final String privacyGroupId = privacyGroup.getPrivacyGroupId();
    assertEquals(privacyGroup.getName(), name);
    assertEquals(privacyGroup.getDescription(), description);

    // find the created privacy group in first node
    final PrivacyGroup[] firstNodePrivacyGroups = findPrivacyGroup(firstNode, addresses);

    assertEquals(firstNodePrivacyGroups.length, 1);
    assertEquals(firstNodePrivacyGroups[0].getPrivacyGroupId(), privacyGroupId);

    // find the created privacy group in second node
    await().atMost(20, TimeUnit.SECONDS).until(
        () -> findPrivacyGroup(secondNode, addresses).length == firstNodePrivacyGroups.length);
    assertEquals(
        findPrivacyGroup(secondNode, addresses)[0].getPrivacyGroupId(),
        firstNodePrivacyGroups[0].getPrivacyGroupId());
  }

  @Test
  void createAndRetrieve() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);
    final EthClientStub secondNode = NodeUtils.client(secondOrionLauncher.clientPort(), secondHttpClient);

    final String name = "testName";
    final String description = "testDescription";
    final String[] addresses = new String[] {PK_1_B_64, PK_2_B_64};
    // create a privacy group
    final PrivacyGroup privacyGroup = NodeUtils.createPrivacyGroup(firstNode, addresses, PK_1_B_64, name, description);

    final String privacyGroupId = privacyGroup.getPrivacyGroupId();
    assertThat(privacyGroup.getName()).isEqualTo(name);
    assertThat(privacyGroup.getDescription()).isEqualTo(description);

    // get the created privacy group in first node
    final PrivacyGroup firstNodePrivacyGroup = retrievePrivacyGroupTransaction(firstNode, privacyGroupId);

    assertThat(firstNodePrivacyGroup.getPrivacyGroupId()).isEqualTo(privacyGroupId);
    assertThat(firstNodePrivacyGroup.getDescription()).isEqualTo(description);
    assertThat(firstNodePrivacyGroup.getName()).isEqualTo(name);
    assertThat(firstNodePrivacyGroup.getMembers()).isEqualTo(addresses);

    // get the created privacy group in second node
    await().atMost(20, TimeUnit.SECONDS).until(
        () -> retrievePrivacyGroupTransaction(secondNode, privacyGroupId).getPrivacyGroupId().equals(privacyGroupId));
    final PrivacyGroup secondNodePrivacyGroup = retrievePrivacyGroupTransaction(secondNode, privacyGroupId);
    assertThat(secondNodePrivacyGroup).isEqualToComparingFieldByField(firstNodePrivacyGroup);
  }

  @Test
  void createDeleteFind() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);
    final EthClientStub secondNode = NodeUtils.client(secondOrionLauncher.clientPort(), secondHttpClient);

    final String name = "testName";
    final String description = "testDescription";
    final String[] addresses = new String[] {PK_1_B_64, PK_2_B_64};
    // create a privacy group
    final PrivacyGroup privacyGroup = createPrivacyGroup(firstNode, addresses, PK_1_B_64, name, description);

    final String privacyGroupId = privacyGroup.getPrivacyGroupId();
    assertEquals(privacyGroup.getName(), name);
    assertEquals(privacyGroup.getDescription(), description);

    // delete privacy group
    final String privacyGroupDeleted = deletePrivacyGroup(firstNode, privacyGroupId, PK_1_B_64);

    // find the created privacy group deleted in first node
    final PrivacyGroup[] deleteNodeFirstPrivacyGroups = findPrivacyGroup(firstNode, addresses);

    final List<String> listFirst =
        Arrays.stream(deleteNodeFirstPrivacyGroups).map(PrivacyGroup::getPrivacyGroupId).collect(Collectors.toList());
    assertFalse(listFirst.contains(privacyGroupDeleted));

    // find the deleted privacy group in second node
    final PrivacyGroup[] deleteNodeSecondPrivacyGroups = findPrivacyGroup(secondNode, addresses);

    final List<String> listSecond =
        Arrays.stream(deleteNodeSecondPrivacyGroups).map(PrivacyGroup::getPrivacyGroupId).collect(Collectors.toList());
    assertFalse(listSecond.contains(privacyGroupDeleted));
  }

  @Test
  void createTwiceDeleteOnce() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);
    final EthClientStub secondNode = NodeUtils.client(secondOrionLauncher.clientPort(), secondHttpClient);

    final String name = "testName";
    final String description = "testDescription";
    final String[] addresses = new String[] {PK_1_B_64, PK_2_B_64};

    // create a privacy group
    final PrivacyGroup firstPrivacyGroup = createPrivacyGroup(firstNode, addresses, PK_1_B_64, name, description);

    final String firstPrivacyGroupId = firstPrivacyGroup.getPrivacyGroupId();
    assertEquals(firstPrivacyGroup.getName(), name);
    assertEquals(firstPrivacyGroup.getDescription(), description);

    // find the created privacy group in first node
    final PrivacyGroup[] initialPrivacyGroups = findPrivacyGroup(firstNode, addresses);
    assertEquals(initialPrivacyGroups.length, 1);
    assertEquals(initialPrivacyGroups[0].getPrivacyGroupId(), firstPrivacyGroupId);


    //create another privacy group
    final PrivacyGroup secondPrivacyGroup = createPrivacyGroup(firstNode, addresses, PK_1_B_64, name, description);

    final String secondPrivacyGroupId = secondPrivacyGroup.getPrivacyGroupId();
    assertEquals(firstPrivacyGroup.getName(), name);
    assertEquals(firstPrivacyGroup.getDescription(), description);

    // find the created privacy group in second node
    await().atMost(5, TimeUnit.SECONDS).until(() -> findPrivacyGroup(secondNode, addresses).length == 2);
    final PrivacyGroup[] updatedPrivacyGroups = findPrivacyGroup(secondNode, addresses);
    assertEquals(updatedPrivacyGroups[0].getPrivacyGroupId(), firstPrivacyGroupId);
    assertEquals(updatedPrivacyGroups[1].getPrivacyGroupId(), secondPrivacyGroupId);

    // delete the first privacy group
    deletePrivacyGroup(firstNode, firstPrivacyGroupId, PK_1_B_64);

    // find the deleted privacy group in second node
    final PrivacyGroup[] deleteNodeSecondPrivacyGroups = findPrivacyGroup(secondNode, addresses);

    final List<String> listSecond =
        Arrays.stream(deleteNodeSecondPrivacyGroups).map(PrivacyGroup::getPrivacyGroupId).collect(Collectors.toList());
    assertFalse(listSecond.contains(firstPrivacyGroupId));
    assertTrue(listSecond.contains(secondPrivacyGroupId));
  }

  @Test
  void createAndDeleteTwice() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);

    final String name = "testName";
    final String description = "testDescription";
    final String[] addresses = new String[] {PK_1_B_64, PK_2_B_64};

    // create a privacy group
    final PrivacyGroup privacyGroup = createPrivacyGroup(firstNode, addresses, PK_1_B_64, name, description);

    final String privacyGroupId = privacyGroup.getPrivacyGroupId();
    assertEquals(privacyGroup.getName(), name);
    assertEquals(privacyGroup.getDescription(), description);

    // find the created privacy group in first node
    final PrivacyGroup[] initialPrivacyGroups = findPrivacyGroup(firstNode, addresses);
    assertEquals(initialPrivacyGroups.length, 1);
    assertEquals(initialPrivacyGroups[0].getPrivacyGroupId(), privacyGroupId);

    // delete the privacy group
    deletePrivacyGroup(firstNode, privacyGroupId, PK_1_B_64);

    // try to delete the group again
    assertThrows(AssertionFailedError.class, () -> deletePrivacyGroup(firstNode, privacyGroupId, PK_1_B_64));
  }

  @Test
  void legacyPrivacyGroupAddedToQueryPrivacyGroupIfNonLegacyGroupHasBeenCreatedBefore() {
    final String[] addresses = new String[] {PK_1_B_64, PK_2_B_64};

    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);
    NodeUtils.client(secondOrionLauncher.clientPort(), secondHttpClient);

    final PrivacyGroup privacyGroup =
        createPrivacyGroup(firstNode, addresses, PK_1_B_64, "nonLegacy", "nonLegacy Group");

    // legacy transaction should create legacy privacy group
    sendTransaction(firstNode, PK_1_B_64, PK_2_B_64);

    final PrivacyGroup[] privacyGroups = findPrivacyGroup(firstNode, addresses);

    assertThat(privacyGroups.length).isEqualTo(2);
    assertThat(
        Arrays
            .stream(privacyGroups)
            .filter(p -> privacyGroup.getPrivacyGroupId().equals(p.getPrivacyGroupId()))
            .collect(Collectors.toList())
            .size()).isEqualTo(1);
    assertThat(
        Arrays.stream(privacyGroups).filter(p -> p.getName().equals("legacy")).collect(Collectors.toList()).size())
            .isEqualTo(1);
  }
}
