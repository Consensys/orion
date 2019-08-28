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
import static net.consensys.cava.io.Base64.decodeBytes;
import static net.consensys.cava.io.file.Files.copyResource;
import static net.consensys.orion.acceptance.NodeUtils.createPrivacyGroupTransaction;
import static net.consensys.orion.acceptance.NodeUtils.deletePrivacyGroupTransaction;
import static net.consensys.orion.acceptance.NodeUtils.findPrivacyGroupTransaction;
import static net.consensys.orion.acceptance.NodeUtils.joinPathsAsTomlListEntry;
import static net.consensys.orion.http.server.HttpContentType.CBOR;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.acceptance.EthClientStub;
import net.consensys.orion.acceptance.NodeUtils;
import net.consensys.orion.cmd.Orion;
import net.consensys.orion.config.Config;
import net.consensys.orion.http.handler.privacy.PrivacyGroup;
import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.network.ConcurrentNetworkNodes;
import net.consensys.orion.utils.Serializer;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Runs up a two nodes that communicates with each other. */
@ExtendWith(TempDirectoryExtension.class)
class DualNodesPrivacyGroupsTest {

  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String PK_2_B_64 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";

  private Config firstNodeConfig;
  private Config secondNodeConfig;
  private ConcurrentNetworkNodes networkNodes;

  private Orion firstOrionLauncher;
  private Orion secondOrionLauncher;
  private Vertx vertx;
  private HttpClient firstHttpClient;
  private HttpClient secondHttpClient;

  @BeforeEach
  void setUpDualNodes(@TempDirectory Path tempDir) throws Exception {

    Path key1pub = copyResource("key1.pub", tempDir.resolve("key1.pub"));
    Path key1key = copyResource("key1.key", tempDir.resolve("key1.key"));
    Path key2pub = copyResource("key2.pub", tempDir.resolve("key2.pub"));
    Path key2key = copyResource("key2.key", tempDir.resolve("key2.key"));
    String jdbcUrl = "jdbc:h2:" + tempDir.resolve("node2").toString();
    try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
      Statement st = conn.createStatement();
      st.executeUpdate("create table if not exists store(key char(60), value binary, primary key(key))");
    }

    firstNodeConfig = NodeUtils.nodeConfig(
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
        "leveldb:database/node1");
    secondNodeConfig = NodeUtils.nodeConfig(
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
        "sql:" + jdbcUrl);

    vertx = vertx();
    firstOrionLauncher = NodeUtils.startOrion(firstNodeConfig);
    firstHttpClient = vertx.createHttpClient();
    secondOrionLauncher = NodeUtils.startOrion(secondNodeConfig);
    secondHttpClient = vertx.createHttpClient();
    Box.PublicKey pk1 = Box.PublicKey.fromBytes(decodeBytes(PK_1_B_64));
    Box.PublicKey pk2 = Box.PublicKey.fromBytes(decodeBytes(PK_2_B_64));
    networkNodes = new ConcurrentNetworkNodes(NodeUtils.url("127.0.0.1", firstOrionLauncher.nodePort()));

    networkNodes.addNode(pk1, NodeUtils.url("127.0.0.1", firstOrionLauncher.nodePort()));
    networkNodes.addNode(pk2, NodeUtils.url("127.0.0.1", secondOrionLauncher.nodePort()));
    // prepare /partyinfo payload (our known peers)
    RequestBody partyInfoBody =
        RequestBody.create(MediaType.parse(CBOR.httpHeaderValue), Serializer.serialize(CBOR, networkNodes));
    // call http endpoint
    OkHttpClient httpClient = new OkHttpClient();

    final String firstNodeBaseUrl = NodeUtils.urlString("127.0.0.1", firstOrionLauncher.nodePort());
    Request request = new Request.Builder().post(partyInfoBody).url(firstNodeBaseUrl + "/partyinfo").build();
    // first /partyinfo call may just get the one node, so wait until we get at least 2 nodes
    await().atMost(5, TimeUnit.SECONDS).until(() -> getPartyInfoResponse(httpClient, request).nodeURLs().size() == 2);

  }

  private ConcurrentNetworkNodes getPartyInfoResponse(OkHttpClient httpClient, Request request) throws Exception {
    Response resp = httpClient.newCall(request).execute();
    assertEquals(200, resp.code());

    ConcurrentNetworkNodes partyInfoResponse =
        Serializer.deserialize(HttpContentType.CBOR, ConcurrentNetworkNodes.class, resp.body().bytes());
    return partyInfoResponse;
  }

  @AfterEach
  void tearDown() {
    firstOrionLauncher.stop();
    secondOrionLauncher.stop();
    vertx.close();
  }

  @Test
  void createAndFind() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);
    final EthClientStub secondNode = NodeUtils.client(secondOrionLauncher.clientPort(), secondHttpClient);

    String name = "testName";
    String description = "testDescription";
    String[] addresses = new String[] {PK_1_B_64, PK_2_B_64};
    // create a privacy group
    final PrivacyGroup privacyGroup = createPrivacyGroupTransaction(firstNode, addresses, PK_1_B_64, name, description);

    String privacyGroupId = privacyGroup.getPrivacyGroupId();
    assertEquals(privacyGroup.getName(), name);
    assertEquals(privacyGroup.getDescription(), description);

    // find the created privacy group in first node
    final PrivacyGroup[] firstNodePrivacyGroups = findPrivacyGroupTransaction(firstNode, addresses);

    assertEquals(firstNodePrivacyGroups.length, 1);
    assertEquals(firstNodePrivacyGroups[0].getPrivacyGroupId(), privacyGroupId);

    // find the created privacy group in second node
    final PrivacyGroup[] secondNodePrivacyGroups = findPrivacyGroupTransaction(secondNode, addresses);

    assertEquals(secondNodePrivacyGroups.length, firstNodePrivacyGroups.length);
    assertEquals(secondNodePrivacyGroups[0].getPrivacyGroupId(), firstNodePrivacyGroups[0].getPrivacyGroupId());
  }

  @Test
  void createDeleteFind() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);
    final EthClientStub secondNode = NodeUtils.client(secondOrionLauncher.clientPort(), secondHttpClient);

    String name = "testName";
    String description = "testDescription";
    String[] addresses = new String[] {PK_1_B_64, PK_2_B_64};
    // create a privacy group
    final PrivacyGroup privacyGroup = createPrivacyGroupTransaction(firstNode, addresses, PK_1_B_64, name, description);

    String privacyGroupId = privacyGroup.getPrivacyGroupId();
    assertEquals(privacyGroup.getName(), name);
    assertEquals(privacyGroup.getDescription(), description);

    // delete privacy group
    String privacyGroupDeleted = deletePrivacyGroupTransaction(firstNode, privacyGroupId, PK_1_B_64);

    // find the created privacy group deleted in first node
    final PrivacyGroup[] deleteNodeFirstPrivacyGroups = findPrivacyGroupTransaction(firstNode, addresses);

    List<String> listFirst =
        Arrays.stream(deleteNodeFirstPrivacyGroups).map(PrivacyGroup::getPrivacyGroupId).collect(Collectors.toList());
    assertFalse(listFirst.contains(privacyGroupDeleted));

    // find the deleted privacy group in second node
    final PrivacyGroup[] deleteNodeSecondPrivacyGroups = findPrivacyGroupTransaction(secondNode, addresses);

    List<String> listSecond =
        Arrays.stream(deleteNodeSecondPrivacyGroups).map(PrivacyGroup::getPrivacyGroupId).collect(Collectors.toList());
    assertFalse(listSecond.contains(privacyGroupDeleted));
  }


  @Test
  void createTwiceDeleteOnce() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);
    final EthClientStub secondNode = NodeUtils.client(secondOrionLauncher.clientPort(), secondHttpClient);

    String name = "testName";
    String description = "testDescription";
    String[] addresses = new String[] {PK_1_B_64, PK_2_B_64};

    // create a privacy group
    PrivacyGroup firstPrivacyGroup = createPrivacyGroupTransaction(firstNode, addresses, PK_1_B_64, name, description);

    String firstPrivacyGroupId = firstPrivacyGroup.getPrivacyGroupId();
    assertEquals(firstPrivacyGroup.getName(), name);
    assertEquals(firstPrivacyGroup.getDescription(), description);

    // find the created privacy group in first node
    PrivacyGroup[] initialPrivacyGroups = findPrivacyGroupTransaction(firstNode, addresses);
    assertEquals(initialPrivacyGroups.length, 1);
    assertEquals(initialPrivacyGroups[0].getPrivacyGroupId(), firstPrivacyGroupId);


    //create another privacy group
    PrivacyGroup secondPrivacyGroup = createPrivacyGroupTransaction(firstNode, addresses, PK_1_B_64, name, description);

    String secondPrivacyGroupId = secondPrivacyGroup.getPrivacyGroupId();
    assertEquals(firstPrivacyGroup.getName(), name);
    assertEquals(firstPrivacyGroup.getDescription(), description);

    // find the created privacy group in second node
    await().atMost(5, TimeUnit.SECONDS).until(() -> findPrivacyGroupTransaction(secondNode, addresses).length == 2);
    PrivacyGroup[] updatedPrivacyGroups = findPrivacyGroupTransaction(secondNode, addresses);
    assertEquals(updatedPrivacyGroups[0].getPrivacyGroupId(), firstPrivacyGroupId);
    assertEquals(updatedPrivacyGroups[1].getPrivacyGroupId(), secondPrivacyGroupId);

    // delete the first privacy group
    deletePrivacyGroupTransaction(firstNode, firstPrivacyGroupId, PK_1_B_64);

    // find the deleted privacy group in second node
    final PrivacyGroup[] deleteNodeSecondPrivacyGroups = findPrivacyGroupTransaction(secondNode, addresses);

    List<String> listSecond =
        Arrays.stream(deleteNodeSecondPrivacyGroups).map(PrivacyGroup::getPrivacyGroupId).collect(Collectors.toList());
    assertFalse(listSecond.contains(firstPrivacyGroupId));
    assertTrue(listSecond.contains(secondPrivacyGroupId));
  }

  @Test
  void createAndDeleteTwice() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);

    String name = "testName";
    String description = "testDescription";
    String[] addresses = new String[] {PK_1_B_64, PK_2_B_64};

    // create a privacy group
    PrivacyGroup privacyGroup = createPrivacyGroupTransaction(firstNode, addresses, PK_1_B_64, name, description);

    String privacyGroupId = privacyGroup.getPrivacyGroupId();
    assertEquals(privacyGroup.getName(), name);
    assertEquals(privacyGroup.getDescription(), description);

    // find the created privacy group in first node
    PrivacyGroup[] initialPrivacyGroups = findPrivacyGroupTransaction(firstNode, addresses);
    assertEquals(initialPrivacyGroups.length, 1);
    assertEquals(initialPrivacyGroups[0].getPrivacyGroupId(), privacyGroupId);

    // delete the privacy group
    deletePrivacyGroupTransaction(firstNode, privacyGroupId, PK_1_B_64);

    // try to delete the group again
    assertThrows(AssertionFailedError.class, () -> deletePrivacyGroupTransaction(firstNode, privacyGroupId, PK_1_B_64));
  }
}
