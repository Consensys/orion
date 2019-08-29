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
import static net.consensys.orion.acceptance.NodeUtils.joinPathsAsTomlListEntry;
import static net.consensys.orion.http.server.HttpContentType.CBOR;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.junit.TempDirectory;
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
import java.util.concurrent.TimeUnit;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

class PrivacyGroupAcceptanceTest {

  static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  static final String PK_2_B_64 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";
  static final String PK_3_B_64 = "6pFBKvouUVdoXRownMEHrRl8SbLJXIZC0EQOmC+WBlg=";

  private Config firstNodeConfig;
  private Config secondNodeConfig;
  private Config thirdNodeConfig;
  private ConcurrentNetworkNodes networkNodes;

  Orion firstOrionLauncher;
  Orion secondOrionLauncher;
  Orion thirdOrionLauncher;
  private Vertx vertx;
  HttpClient firstHttpClient;
  HttpClient secondHttpClient;
  HttpClient thirdHttpClient;

  @BeforeEach
  void setUpTriNodes(@TempDirectory Path tempDir) throws Exception {

    Path key1pub = copyResource("key1.pub", tempDir.resolve("key1.pub"));
    Path key1key = copyResource("key1.key", tempDir.resolve("key1.key"));
    Path key2pub = copyResource("key2.pub", tempDir.resolve("key2.pub"));
    Path key2key = copyResource("key2.key", tempDir.resolve("key2.key"));
    Path key3pub = copyResource("key3.pub", tempDir.resolve("key3.pub"));
    Path key3key = copyResource("key3.key", tempDir.resolve("key3.key"));

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
    thirdNodeConfig = NodeUtils.nodeConfig(
        tempDir,
        0,
        "127.0.0.1",
        0,
        "127.0.0.1",
        "node3",
        joinPathsAsTomlListEntry(key3pub),
        joinPathsAsTomlListEntry(key3key),
        "off",
        "tofu",
        "tofu",
        "leveldb:database/node3");

    vertx = vertx();
    firstOrionLauncher = NodeUtils.startOrion(firstNodeConfig);
    firstHttpClient = vertx.createHttpClient();
    secondOrionLauncher = NodeUtils.startOrion(secondNodeConfig);
    secondHttpClient = vertx.createHttpClient();
    thirdOrionLauncher = NodeUtils.startOrion(thirdNodeConfig);
    thirdHttpClient = vertx.createHttpClient();

    Box.PublicKey pk1 = Box.PublicKey.fromBytes(decodeBytes(PK_1_B_64));
    Box.PublicKey pk2 = Box.PublicKey.fromBytes(decodeBytes(PK_2_B_64));
    Box.PublicKey pk3 = Box.PublicKey.fromBytes(decodeBytes(PK_3_B_64));

    networkNodes = new ConcurrentNetworkNodes(NodeUtils.url("127.0.0.1", firstOrionLauncher.nodePort()));
    networkNodes.addNode(pk1, NodeUtils.url("127.0.0.1", firstOrionLauncher.nodePort()));
    networkNodes.addNode(pk2, NodeUtils.url("127.0.0.1", secondOrionLauncher.nodePort()));
    networkNodes.addNode(pk3, NodeUtils.url("127.0.0.1", thirdOrionLauncher.nodePort()));
    // prepare /partyinfo payload (our known peers)
    RequestBody partyInfoBody =
        RequestBody.create(MediaType.parse(CBOR.httpHeaderValue), Serializer.serialize(CBOR, networkNodes));
    // call http endpoint
    OkHttpClient httpClient = new OkHttpClient();

    final String firstNodeBaseUrl = NodeUtils.urlString("127.0.0.1", firstOrionLauncher.nodePort());
    Request request = new Request.Builder().post(partyInfoBody).url(firstNodeBaseUrl + "/partyinfo").build();
    // first /partyinfo call may just get the one node, so wait until we get exactly 3 nodes
    await().atMost(5, TimeUnit.SECONDS).until(() -> getPartyInfoResponse(httpClient, request).nodeURLs().size() == 3);
  }

  private ConcurrentNetworkNodes getPartyInfoResponse(OkHttpClient httpClient, Request request) throws Exception {
    Response resp = httpClient.newCall(request).execute();
    assertEquals(200, resp.code());
    return Serializer.deserialize(HttpContentType.CBOR, ConcurrentNetworkNodes.class, resp.body().bytes());
  }

  @AfterEach
  void tearDown() {
    Awaitility.await().until(() -> {
      try {
        firstOrionLauncher.stop();
        secondOrionLauncher.stop();
        thirdOrionLauncher.stop();
        return true;
      } catch (Exception ignored) {
        return false;
      }
    });
    vertx.close();
  }

}
