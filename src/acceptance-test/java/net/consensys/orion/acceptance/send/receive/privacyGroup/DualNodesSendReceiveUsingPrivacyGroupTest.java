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
import static net.consensys.cava.io.Base64.encodeBytes;
import static net.consensys.cava.io.file.Files.copyResource;
import static net.consensys.orion.acceptance.NodeUtils.assertTransaction;
import static net.consensys.orion.acceptance.NodeUtils.joinPathsAsTomlListEntry;
import static net.consensys.orion.acceptance.NodeUtils.sendTransaction;
import static net.consensys.orion.acceptance.NodeUtils.sendTransactionPrivacyGroupId;
import static net.consensys.orion.acceptance.NodeUtils.viewTransaction;
import static net.consensys.orion.acceptance.NodeUtils.viewTransactionPrivacyGroupId;
import static net.consensys.orion.http.server.HttpContentType.CBOR;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.acceptance.EthClientStub;
import net.consensys.orion.acceptance.NodeUtils;
import net.consensys.orion.cmd.Orion;
import net.consensys.orion.config.Config;
import net.consensys.orion.http.handler.receive.ReceiveResponse;
import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.network.ConcurrentNetworkNodes;
import net.consensys.orion.utils.Serializer;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
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
class DualNodesSendReceiveUsingPrivacyGroupTest {

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
  void receiverCanViewWhenSentToPrivacyGroup() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);
    final EthClientStub secondNode = NodeUtils.client(secondOrionLauncher.clientPort(), secondHttpClient);

    final String digest = sendTransaction(firstNode, PK_1_B_64, PK_2_B_64);
    final ReceiveResponse receivedPayload = viewTransactionPrivacyGroupId(firstNode, PK_1_B_64, digest);

    assertTransaction(receivedPayload.getPayload());

    final String digestPrivacyGroup =
        sendTransactionPrivacyGroupId(firstNode, PK_1_B_64, encodeBytes(receivedPayload.getPrivacyGroupId()));
    final byte[] response = viewTransaction(secondNode, PK_2_B_64, digestPrivacyGroup);

    assertTransaction(response);
  }

  @Test
  void senderCanViewWhenSentToPrivacyGroup() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);

    final String digest = sendTransaction(firstNode, PK_1_B_64, PK_2_B_64);
    final ReceiveResponse receivedPayload = viewTransactionPrivacyGroupId(firstNode, PK_1_B_64, digest);

    assertTransaction(receivedPayload.getPayload());

    final String digestPriv =
        sendTransactionPrivacyGroupId(firstNode, PK_1_B_64, encodeBytes(receivedPayload.getPrivacyGroupId()));
    final byte[] response = viewTransaction(firstNode, PK_1_B_64, digestPriv);

    assertTransaction(response);

  }
}
