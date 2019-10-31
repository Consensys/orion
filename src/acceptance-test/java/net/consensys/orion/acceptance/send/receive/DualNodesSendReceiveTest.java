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
package net.consensys.orion.acceptance.send.receive;

import static io.vertx.core.Vertx.vertx;
import static net.consensys.cava.io.Base64.decodeBytes;
import static net.consensys.cava.io.file.Files.copyResource;
import static net.consensys.orion.acceptance.NodeUtils.assertTransaction;
import static net.consensys.orion.acceptance.NodeUtils.joinPathsAsTomlListEntry;
import static net.consensys.orion.acceptance.NodeUtils.sendTransaction;
import static net.consensys.orion.acceptance.NodeUtils.viewTransaction;
import static net.consensys.orion.http.server.HttpContentType.CBOR;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.acceptance.EthClientStub;
import net.consensys.orion.acceptance.NodeUtils;
import net.consensys.orion.cmd.Orion;
import net.consensys.orion.config.Config;
import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.network.ConcurrentNetworkNodes;
import net.consensys.orion.utils.Serializer;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

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
class DualNodesSendReceiveTest {

  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String PK_2_B_64 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";
  private static final String PK_3_B_64 = "SANQhg9hXMn0xcbll6wFcE6ICPKZjsC8gHg8YpmG+m0=";
  private static final String PK_4_B_64 = "LSHik/TRY49hCZ/CQjQHCvd8VZwJxTbX+9594mBK4nM=";
  private static final String PK_5_B_64 = "FEPb+LDOZ3E+kxX2RnNqygG2QlNPaGX9xrhD16rSWwQ=";

  private Config firstNodeConfig;
  private Config secondNodeConfig;
  private ConcurrentNetworkNodes networkNodes;

  private Orion firstOrionLauncher;
  private Orion secondOrionLauncher;
  private Vertx vertx;
  private HttpClient firstHttpClient;
  private HttpClient secondHttpClient;

  @BeforeEach
  void setUpDualNodes(@TempDirectory final Path tempDir) throws Exception {

    final Path key1pub = copyResource("key1.pub", tempDir.resolve("key1.pub"));
    final Path key1key = copyResource("key1.key", tempDir.resolve("key1.key"));
    final Path key2pub = copyResource("key2.pub", tempDir.resolve("key2.pub"));
    final Path key2key = copyResource("key2.key", tempDir.resolve("key2.key"));
    final Path key3pub = copyResource("key3.pub", tempDir.resolve("key3.pub"));
    final Path key3key = copyResource("key3.key", tempDir.resolve("key3.key"));
    final Path key4pub = copyResource("key4.pub", tempDir.resolve("key4.pub"));
    final Path key4key = copyResource("key4.key", tempDir.resolve("key4.key"));
    final Path key5pub = copyResource("key5.pub", tempDir.resolve("key5.pub"));
    final Path key5key = copyResource("key5.key", tempDir.resolve("key5.key"));

    final String jdbcUrl = "jdbc:h2:" + tempDir.resolve("node2").toString();
    try (final Connection conn = DriverManager.getConnection(jdbcUrl)) {
      final Statement st = conn.createStatement();
      st.executeUpdate("create table if not exists store(key char(60), value binary, primary key(key))");
    }


    firstNodeConfig = NodeUtils.nodeConfig(
        tempDir,
        0,
        "127.0.0.1",
        0,
        "127.0.0.1",
        "node1",
        joinPathsAsTomlListEntry(key1pub, key2pub),
        joinPathsAsTomlListEntry(key1key, key2key),
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
        joinPathsAsTomlListEntry(key3pub, key4pub, key5pub),
        joinPathsAsTomlListEntry(key3key, key4key, key5key),
        "off",
        "tofu",
        "tofu",
        "sql:" + jdbcUrl);
    vertx = vertx();
    firstOrionLauncher = NodeUtils.startOrion(firstNodeConfig);
    firstHttpClient = vertx.createHttpClient();
    secondOrionLauncher = NodeUtils.startOrion(secondNodeConfig);
    secondHttpClient = vertx.createHttpClient();
    final Box.PublicKey pk1 = Box.PublicKey.fromBytes(decodeBytes(PK_1_B_64));
    final Box.PublicKey pk2 = Box.PublicKey.fromBytes(decodeBytes(PK_2_B_64));
    final Box.PublicKey pk3 = Box.PublicKey.fromBytes(decodeBytes(PK_3_B_64));
    final Box.PublicKey pk4 = Box.PublicKey.fromBytes(decodeBytes(PK_4_B_64));
    final Box.PublicKey pk5 = Box.PublicKey.fromBytes(decodeBytes(PK_5_B_64));
    networkNodes = new ConcurrentNetworkNodes(NodeUtils.url("127.0.0.1", firstOrionLauncher.nodePort()));

    networkNodes.addNode(
        Arrays.asList(new Box.PublicKey[] {pk1, pk2}),
        NodeUtils.url("127.0.0.1", firstOrionLauncher.nodePort()));
    networkNodes.addNode(
        Arrays.asList(new Box.PublicKey[] {pk3, pk4, pk5}),
        NodeUtils.url("127.0.0.1", secondOrionLauncher.nodePort()));
    // prepare /partyinfo payload (our known peers)
    final RequestBody partyInfoBody =
        RequestBody.create(MediaType.parse(CBOR.httpHeaderValue), Serializer.serialize(CBOR, networkNodes));
    // call http endpoint
    final OkHttpClient httpClient = new OkHttpClient();

    final String firstNodeBaseUrl = NodeUtils.urlString("127.0.0.1", firstOrionLauncher.nodePort());
    final Request request = new Request.Builder().post(partyInfoBody).url(firstNodeBaseUrl + "/partyinfo").build();
    // first /partyinfo call may just get the one node, so wait until we get at least 2 nodes
    await().atMost(5, TimeUnit.SECONDS).until(() -> getPartyInfoResponse(httpClient, request).nodeURLs().size() == 2);

  }

  private ConcurrentNetworkNodes getPartyInfoResponse(final OkHttpClient httpClient, final Request request)
      throws Exception {
    final Response resp = httpClient.newCall(request).execute();
    assertEquals(200, resp.code());

    final ConcurrentNetworkNodes partyInfoResponse =
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
  void receiverCanView() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);
    final EthClientStub secondNode = NodeUtils.client(secondOrionLauncher.clientPort(), secondHttpClient);

    final String digest = sendTransaction(firstNode, PK_1_B_64, PK_3_B_64);
    final byte[] receivedPayload = viewTransaction(secondNode, PK_3_B_64, digest);

    assertTransaction(receivedPayload);
  }

  @Test
  void senderCanView() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);

    final String digest = sendTransaction(firstNode, PK_1_B_64, PK_3_B_64);
    final byte[] receivedPayload = viewTransaction(firstNode, PK_1_B_64, digest);

    assertTransaction(receivedPayload);
  }

  @Test
  void sendToMultipleRecipientsOnOneOrionUsesAllRecipientsKeysToEncryptButStripsAllOtherNodesKeys() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);
    final EthClientStub secondNode = NodeUtils.client(secondOrionLauncher.clientPort(), secondHttpClient);

    final String digest = sendTransaction(firstNode, PK_1_B_64, PK_4_B_64, PK_5_B_64);
    byte[] receivedPayload = viewTransaction(secondNode, PK_4_B_64, digest);
    assertTransaction(receivedPayload);

    receivedPayload = viewTransaction(secondNode, PK_5_B_64, digest);
    assertTransaction(receivedPayload);

    assertThatThrownBy(() -> viewTransaction(secondNode, PK_3_B_64, digest)).isInstanceOf(AssertionFailedError.class);

    assertThatThrownBy(() -> viewTransaction(secondNode, PK_1_B_64, digest)).isInstanceOf(AssertionFailedError.class);

    receivedPayload = viewTransaction(firstNode, PK_1_B_64, digest);
    assertTransaction(receivedPayload);

    assertThatThrownBy(() -> viewTransaction(firstNode, PK_4_B_64, digest)).isInstanceOf(AssertionFailedError.class);

    assertThatThrownBy(() -> viewTransaction(firstNode, PK_5_B_64, digest)).isInstanceOf(AssertionFailedError.class);
  }

  @Test
  void receiveWithoutToCanView() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);
    final EthClientStub secondNode = NodeUtils.client(secondOrionLauncher.clientPort(), secondHttpClient);

    String digest = sendTransaction(firstNode, PK_1_B_64, PK_5_B_64);

    byte[] receivedPayload = viewTransaction(secondNode, null, digest);
    assertTransaction(receivedPayload);

    receivedPayload = viewTransaction(firstNode, null, digest);
    assertTransaction(receivedPayload);

    digest = sendTransaction(firstNode, PK_2_B_64, PK_3_B_64);

    receivedPayload = viewTransaction(secondNode, null, digest);
    assertTransaction(receivedPayload);

    receivedPayload = viewTransaction(firstNode, null, digest);
    assertTransaction(receivedPayload);
  }
}
