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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
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

  private List<String> pubKeyStrings;

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

    final List<String> pubKeys = List.of("key1.pub", "key2.pub", "key3.pub", "key4.pub", "key5.pub");
    final List<String> priKeys = List.of("key1.key", "key2.key", "key3.key", "key4.key", "key5.key");
    final List<Path> pubKeyFileList = pubKeys.stream().map(pub -> getPath(tempDir, pub)).collect(Collectors.toList());
    pubKeyStrings = pubKeys.stream().map(pub -> getStringFromResource(pub)).collect(Collectors.toList());
    final List<Path> priKeyFileList = priKeys.stream().map(pub -> getPath(tempDir, pub)).collect(Collectors.toList());

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
        joinPathsAsTomlListEntry(pubKeyFileList.get(0), pubKeyFileList.get(1)),
        joinPathsAsTomlListEntry(priKeyFileList.get(0), priKeyFileList.get(1)),
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
        joinPathsAsTomlListEntry(pubKeyFileList.get(2), pubKeyFileList.get(3), pubKeyFileList.get(4)),
        joinPathsAsTomlListEntry(priKeyFileList.get(2), priKeyFileList.get(3), priKeyFileList.get(4)),
        "off",
        "tofu",
        "tofu",
        "sql:" + jdbcUrl);
    vertx = vertx();
    firstOrionLauncher = NodeUtils.startOrion(firstNodeConfig);
    firstHttpClient = vertx.createHttpClient();
    secondOrionLauncher = NodeUtils.startOrion(secondNodeConfig);
    secondHttpClient = vertx.createHttpClient();
    final Box.PublicKey pk1 = Box.PublicKey.fromBytes(decodeBytes(pubKeyStrings.get(0)));
    final Box.PublicKey pk2 = Box.PublicKey.fromBytes(decodeBytes(pubKeyStrings.get(1)));
    final Box.PublicKey pk3 = Box.PublicKey.fromBytes(decodeBytes(pubKeyStrings.get(2)));
    final Box.PublicKey pk4 = Box.PublicKey.fromBytes(decodeBytes(pubKeyStrings.get(3)));
    final Box.PublicKey pk5 = Box.PublicKey.fromBytes(decodeBytes(pubKeyStrings.get(4)));
    networkNodes = new ConcurrentNetworkNodes(NodeUtils.url("127.0.0.1", firstOrionLauncher.nodePort()));

    networkNodes.addNode(Arrays.asList(pk1, pk2), NodeUtils.url("127.0.0.1", firstOrionLauncher.nodePort()));
    networkNodes.addNode(Arrays.asList(pk3, pk4, pk5), NodeUtils.url("127.0.0.1", secondOrionLauncher.nodePort()));
    // prepare /partyinfo payload (our known peers)
    final RequestBody partyInfoBody =
        RequestBody.create(MediaType.parse(CBOR.httpHeaderValue), Serializer.serialize(CBOR, networkNodes));
    // call http endpoint
    final OkHttpClient httpClient = new OkHttpClient();

    final String firstNodeBaseUrl = NodeUtils.urlString("127.0.0.1", firstOrionLauncher.nodePort());
    final Request request = new Request.Builder().post(partyInfoBody).url(firstNodeBaseUrl + "/partyinfo").build();
    // first /partyinfo call may just get the one node, so wait until we get at least 2 nodes
    await().atMost(10, TimeUnit.SECONDS).until(() -> getPartyInfoResponse(httpClient, request).nodeURLs().size() == 2);

  }

  private Path getPath(@TempDirectory final Path tempDir, final String pub) {
    try {
      return copyResource(pub, tempDir.resolve(pub));
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String getStringFromResource(final String resourceFileName) {
    try {
      final URL resource = Resources.getResource(resourceFileName);
      return Resources.toString(resource, Charsets.UTF_8);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
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

    final String digest = sendTransaction(firstNode, pubKeyStrings.get(0), pubKeyStrings.get(2));
    final byte[] receivedPayload = viewTransaction(secondNode, pubKeyStrings.get(2), digest);

    assertTransaction(receivedPayload);
  }

  @Test
  void senderCanView() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);

    final String digest = sendTransaction(firstNode, pubKeyStrings.get(0), pubKeyStrings.get(2));
    final byte[] receivedPayload = viewTransaction(firstNode, pubKeyStrings.get(0), digest);

    assertTransaction(receivedPayload);
  }

  @Test
  void sendToMultipleRecipientsOnOneOrionUsesAllRecipientsKeysToEncryptButStripsAllOtherNodesKeys() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);
    final EthClientStub secondNode = NodeUtils.client(secondOrionLauncher.clientPort(), secondHttpClient);

    final String digest = sendTransaction(firstNode, pubKeyStrings.get(0), pubKeyStrings.get(3), pubKeyStrings.get(4));
    byte[] receivedPayload = viewTransaction(secondNode, pubKeyStrings.get(3), digest);
    assertTransaction(receivedPayload);

    receivedPayload = viewTransaction(secondNode, pubKeyStrings.get(4), digest);
    assertTransaction(receivedPayload);

    assertTransactionNotReceived(secondNode, pubKeyStrings.get(2), digest);

    assertTransactionNotReceived(secondNode, pubKeyStrings.get(0), digest);

    receivedPayload = viewTransaction(firstNode, pubKeyStrings.get(0), digest);
    assertTransaction(receivedPayload);

    assertTransactionNotReceived(firstNode, pubKeyStrings.get(3), digest);

    assertTransactionNotReceived(firstNode, pubKeyStrings.get(4), digest);
  }

  private void assertTransactionNotReceived(
      final EthClientStub secondNode,
      final String pubKeyString,
      final String digest) {
    assertThatThrownBy(() -> viewTransaction(secondNode, pubKeyString, digest))
        .isInstanceOf(AssertionFailedError.class);
  }

  @Test
  void receiveWithoutToCanView() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);
    final EthClientStub secondNode = NodeUtils.client(secondOrionLauncher.clientPort(), secondHttpClient);

    String digest = sendTransaction(firstNode, pubKeyStrings.get(0), pubKeyStrings.get(4));

    byte[] receivedPayload = viewTransaction(secondNode, null, digest);
    assertTransaction(receivedPayload);

    receivedPayload = viewTransaction(firstNode, null, digest);
    assertTransaction(receivedPayload);

    digest = sendTransaction(firstNode, pubKeyStrings.get(1), pubKeyStrings.get(2));

    receivedPayload = viewTransaction(secondNode, null, digest);
    assertTransaction(receivedPayload);

    receivedPayload = viewTransaction(firstNode, null, digest);
    assertTransaction(receivedPayload);
  }
}
