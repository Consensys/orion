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
package net.consensys.orion.acceptance.send.receive.privacyGroup.tx;

import static io.vertx.core.Vertx.vertx;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.cava.io.Base64.decodeBytes;
import static net.consensys.cava.io.file.Files.copyResource;
import static net.consensys.orion.acceptance.NodeUtils.createPrivacyGroupTransaction;
import static net.consensys.orion.acceptance.NodeUtils.joinPathsAsTomlListEntry;
import static net.consensys.orion.acceptance.NodeUtils.push;
import static net.consensys.orion.http.server.HttpContentType.CBOR;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.acceptance.EthClientStub;
import net.consensys.orion.acceptance.NodeUtils;
import net.consensys.orion.cmd.Orion;
import net.consensys.orion.config.Config;
import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.enclave.sodium.MemoryKeyStore;
import net.consensys.orion.enclave.sodium.SodiumEnclave;
import net.consensys.orion.http.handler.privacy.PrivacyGroup;
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

@ExtendWith(TempDirectoryExtension.class)
class TxPrivacyGroupTest {

  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String PK_2_B_64 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";

  private Orion firstOrionLauncher;
  private Vertx vertx;
  private HttpClient firstHttpClient;
  private MemoryKeyStore memoryKeyStore;

  @BeforeEach
  void setUpSingleNode(@TempDirectory Path tempDir) throws Exception {
    vertx = vertx();

    Path key1pub = copyResource("key1.pub", tempDir.resolve("key1.pub"));
    Path key1key = copyResource("key1.key", tempDir.resolve("key1.key"));
    Path key2pub = copyResource("key2.pub", tempDir.resolve("key2.pub"));
    Path key2key = copyResource("key2.key", tempDir.resolve("key2.key"));

    String jdbcUrl = "jdbc:h2:" + tempDir.resolve("node2").toString();
    try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
      Statement st = conn.createStatement();
      st.executeUpdate("create table if not exists store(key binary, value binary, primary key(key))");
    }

    Config firstNodeConfig = NodeUtils.nodeConfig(
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

    Config secondNodeConfig = NodeUtils.nodeConfig(
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

    firstOrionLauncher = NodeUtils.startOrion(firstNodeConfig);
    firstHttpClient = vertx.createHttpClient();
    Orion secondOrionLauncher = NodeUtils.startOrion(secondNodeConfig);

    Box.PublicKey pk1 = Box.PublicKey.fromBytes(decodeBytes(PK_1_B_64));
    Box.PublicKey pk2 = Box.PublicKey.fromBytes(decodeBytes(PK_2_B_64));

    ConcurrentNetworkNodes networkNodes =
        new ConcurrentNetworkNodes(NodeUtils.url("127.0.0.1", firstOrionLauncher.nodePort()));
    networkNodes.addNode(pk1, NodeUtils.url("127.0.0.1", firstOrionLauncher.nodePort()));
    networkNodes.addNode(pk2, NodeUtils.url("127.0.0.1", secondOrionLauncher.nodePort()));

    RequestBody partyInfoBody =
        RequestBody.create(MediaType.parse(CBOR.httpHeaderValue), Serializer.serialize(CBOR, networkNodes));
    OkHttpClient httpClient = new OkHttpClient();

    final String firstNodeBaseUrl = NodeUtils.urlString("127.0.0.1", firstOrionLauncher.nodePort());
    Request request = new Request.Builder().post(partyInfoBody).url(firstNodeBaseUrl + "/partyinfo").build();
    await().atMost(5, TimeUnit.SECONDS).until(() -> getPartyInfoResponse(httpClient, request).nodeURLs().size() == 2);

    memoryKeyStore = new MemoryKeyStore();
  }

  private ConcurrentNetworkNodes getPartyInfoResponse(OkHttpClient httpClient, Request request) throws Exception {
    Response resp = httpClient.newCall(request).execute();
    assertEquals(200, resp.code());
    return Serializer.deserialize(HttpContentType.CBOR, ConcurrentNetworkNodes.class, resp.body().bytes());
  }

  @AfterEach
  void tearDown() {
    firstOrionLauncher.stop();
    vertx.close();
  }

  @Test
  void receiverCanViewWhenSentToPrivacyGroup() {
    final EthClientStub firstNode = NodeUtils.client(firstOrionLauncher.clientPort(), firstHttpClient);
    final EthClientStub firstNodeNode = NodeUtils.client(firstOrionLauncher.nodePort(), firstHttpClient);

    String[] addresses = new String[] {PK_1_B_64, PK_2_B_64};
    final PrivacyGroup privacyGroup =
        createPrivacyGroupTransaction(firstNode, addresses, PK_1_B_64, "testName", "testDescription");

    EncryptedPayload payload = mockPayload();
    var pushResult = push(firstNodeNode, payload);

    var result = firstNode.pushToHistory(privacyGroup.getPrivacyGroupId(), "0xnotahash", pushResult);
    assertTrue(result.isPresent() && result.get());
  }


  private EncryptedPayload mockPayload() {
    SodiumEnclave sEnclave = new SodiumEnclave(memoryKeyStore);
    Box.PublicKey k1 = memoryKeyStore.generateKeyPair();
    Box.PublicKey k2 = memoryKeyStore.generateKeyPair();
    return sEnclave.encrypt("something important".getBytes(UTF_8), k1, new Box.PublicKey[] {k2}, null);
  }
}
