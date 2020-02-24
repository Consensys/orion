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
import static net.consensys.orion.acceptance.NodeUtils.findPrivacyGroup;
import static net.consensys.orion.acceptance.NodeUtils.freePort;
import static net.consensys.orion.acceptance.NodeUtils.joinPathsAsTomlListEntry;
import static net.consensys.orion.acceptance.NodeUtils.sendTransaction;
import static org.apache.tuweni.io.file.Files.copyResource;
import static org.junit.Assert.assertEquals;

import net.consensys.orion.acceptance.EthClientStub;
import net.consensys.orion.acceptance.NodeUtils;
import net.consensys.orion.cmd.Orion;
import net.consensys.orion.config.Config;
import net.consensys.orion.http.handler.privacy.PrivacyGroup;

import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import org.apache.tuweni.junit.TempDirectory;
import org.apache.tuweni.junit.TempDirectoryExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Runs up a single client that communicates with itself. */
@ExtendWith(TempDirectoryExtension.class)
public class SingleNodeDualSendReceiveTest {
  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String PK_2_B_64 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";
  private static final String HOST_NAME = "127.0.0.1";

  private static Config config;
  private static int clientPort;

  private Orion orionLauncher;
  private Vertx vertx;
  private HttpClient httpClient;

  @BeforeAll
  static void setUpSingleNode(@TempDirectory final Path tempDir) throws Exception {
    final int nodePort = freePort();
    clientPort = freePort();

    final Path key1pub = copyResource("key1.pub", tempDir.resolve("key1.pub"));
    final Path key1key = copyResource("key1.key", tempDir.resolve("key1.key"));
    final Path key2pub = copyResource("key2.pub", tempDir.resolve("key2.pub"));
    final Path key2key = copyResource("key2.key", tempDir.resolve("key2.key"));

    config = NodeUtils.nodeConfig(
        tempDir,
        nodePort,
        HOST_NAME,
        clientPort,
        HOST_NAME,
        "node1",
        joinPathsAsTomlListEntry(key1pub, key2pub),
        joinPathsAsTomlListEntry(key1key, key2key),
        "off",
        "tofu",
        "tofu",
        "leveldb:database/node1",
        "memory");
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

  @Test
  void dualSendLegacyDoesNotDuplicateGroup() throws Exception {
    final EthClientStub ethClientStub = NodeUtils.client(clientPort, httpClient);

    sendTransaction(ethClientStub, PK_1_B_64, PK_2_B_64);
    sendTransaction(ethClientStub, PK_1_B_64, PK_2_B_64);

    final PrivacyGroup[] firstNodePrivacyGroups = findPrivacyGroup(ethClientStub, new String[] {PK_1_B_64, PK_2_B_64});

    assertEquals(1, firstNodePrivacyGroups.length);
  }
}
