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
import static net.consensys.cava.io.file.Files.copyResource;
import static net.consensys.orion.acceptance.NodeUtils.assertTransaction;
import static net.consensys.orion.acceptance.NodeUtils.freePort;
import static net.consensys.orion.acceptance.NodeUtils.joinPathsAsTomlListEntry;
import static net.consensys.orion.acceptance.NodeUtils.sendTransaction;
import static net.consensys.orion.acceptance.NodeUtils.viewTransaction;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.acceptance.EthClientStub;
import net.consensys.orion.acceptance.NodeUtils;
import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.api.config.Config;

import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Runs up a single client that communicates with itself. */
@ExtendWith(TempDirectoryExtension.class)
class SingleNodeSendReceiveTest {

  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String PK_2_B_64 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";
  private static final String HOST_NAME = "127.0.0.1";

  private static Config config;
  private static int clientPort;

  private Orion orionLauncher;
  private Vertx vertx;
  private HttpClient httpClient;

  @BeforeAll
  static void setUpSingleNode(@TempDirectory Path tempDir) throws Exception {
    final int nodePort = freePort();
    clientPort = freePort();

    String baseUrl = NodeUtils.url(HOST_NAME, nodePort);
    String clientUrl = NodeUtils.url(HOST_NAME, clientPort);

    Path key1pub = copyResource("key1.pub", tempDir.resolve("key1.pub"));
    Path key1key = copyResource("key1.key", tempDir.resolve("key1.key"));
    Path key2pub = copyResource("key2.pub", tempDir.resolve("key2.pub"));
    Path key2key = copyResource("key2.key", tempDir.resolve("key2.key"));

    config = NodeUtils.nodeConfig(
        tempDir,
        baseUrl,
        nodePort,
        "127.0.0.1",
        clientUrl,
        clientPort,
        "127.0.0.1",
        "node1",
        baseUrl,
        joinPathsAsTomlListEntry(key1pub, key2pub),
        joinPathsAsTomlListEntry(key1key, key2key),
        "off",
        "tofu",
        "tofu");
  }

  @BeforeEach
  void setUp() throws ExecutionException, InterruptedException {
    vertx = vertx();
    orionLauncher = NodeUtils.startOrion(config);
    httpClient = vertx.createHttpClient();
  }

  @AfterEach
  void tearDown() {
    orionLauncher.stop();
    vertx.close();
  }

  /** Sender and receiver use the same key. */
  @Test
  void keyIdentity() throws Exception {
    final EthClientStub ethClientStub = NodeUtils.client(clientPort, httpClient);

    final String digest = sendTransaction(ethClientStub, PK_2_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(ethClientStub, PK_2_B_64, digest);

    assertTransaction(receivedPayload);
  }

  /** Different keys for the sender and receiver. */
  @Test
  void receiverCanView() throws Exception {
    final EthClientStub ethClientStub = NodeUtils.client(clientPort, httpClient);

    final String digest = sendTransaction(ethClientStub, PK_1_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(ethClientStub, PK_2_B_64, digest);

    assertTransaction(receivedPayload);
  }

  /** The sender key can view their transaction when not in the recipient key list. */
  @Test
  void senderCanView() throws Exception {
    final EthClientStub ethClientStub = NodeUtils.client(clientPort, httpClient);

    final String digest = sendTransaction(ethClientStub, PK_1_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(ethClientStub, PK_1_B_64, digest);

    assertTransaction(receivedPayload);
  }
}
