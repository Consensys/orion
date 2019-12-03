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
import static net.consensys.cava.io.file.Files.copyResource;
import static net.consensys.orion.acceptance.NodeUtils.createPrivacyGroup;
import static net.consensys.orion.acceptance.NodeUtils.findPrivacyGroup;
import static net.consensys.orion.acceptance.NodeUtils.freePort;
import static net.consensys.orion.acceptance.NodeUtils.joinPathsAsTomlListEntry;
import static org.junit.Assert.assertEquals;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.acceptance.EthClientStub;
import net.consensys.orion.acceptance.NodeUtils;
import net.consensys.orion.cmd.Orion;
import net.consensys.orion.config.Config;
import net.consensys.orion.http.handler.privacy.PrivacyGroup;

import java.nio.file.Path;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
public class QueryGroupRestartTest {
  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
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

    config = NodeUtils.nodeConfig(
        tempDir,
        nodePort,
        HOST_NAME,
        clientPort,
        HOST_NAME,
        "node1",
        joinPathsAsTomlListEntry(key1pub),
        joinPathsAsTomlListEntry(key1key),
        "off",
        "tofu",
        "tofu",
        "leveldb:database/node1");
  }

  @BeforeEach
  void setUp() {
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
  void nodeRestartDoesNotBreakFindPrivacyGroup() {
    final EthClientStub ethClientStub = NodeUtils.client(clientPort, httpClient);

    createPrivacyGroup(ethClientStub, new String[] {PK_1_B_64}, PK_1_B_64, "Test", "Test");

    final PrivacyGroup[] privacyGroups = findPrivacyGroup(ethClientStub, new String[] {PK_1_B_64});

    orionLauncher.stop();
    vertx.close();

    vertx = vertx();
    orionLauncher = NodeUtils.startOrion(config);
    httpClient = vertx.createHttpClient();

    final EthClientStub ethClientStubAfterRestart = NodeUtils.client(clientPort, httpClient);

    final PrivacyGroup[] privacyGroupsAfterRestart =
        findPrivacyGroup(ethClientStubAfterRestart, new String[] {PK_1_B_64});

    assertEquals(1, privacyGroups.length);
    assertEquals(1, privacyGroupsAfterRestart.length);
  }
}
