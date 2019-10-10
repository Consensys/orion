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
package net.consensys.orion.acceptance.send;

import static io.vertx.core.Vertx.vertx;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.cava.io.file.Files.copyResource;
import static net.consensys.orion.acceptance.NodeUtils.joinPathsAsTomlListEntry;
import static org.junit.Assert.assertEquals;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.acceptance.EthClientStub;
import net.consensys.orion.acceptance.NodeUtils;
import net.consensys.orion.cmd.Orion;
import net.consensys.orion.config.Config;
import net.consensys.orion.exception.OrionErrorCode;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Runs up a single client that communicates with itself. */
@ExtendWith(TempDirectoryExtension.class)
class SingleNodeSendTest {

  private static final byte[] originalPayload = "another wonderful transaction".getBytes(UTF_8);

  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String PK_2_B_64 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";
  private static final String PK_CORRUPTED = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGoAAA=";
  private static final String PK_MISSING_PEER = "A1aVtMxLCUlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";
  private static final String HOST_NAME = "127.0.0.1";

  private static Config config;

  private Vertx vertx;
  private Orion orionLauncher;
  private HttpClient httpClient;

  @BeforeAll
  static void setUpSingleNode(@TempDirectory final Path tempDir) throws Exception {
    final Path key1pub = copyResource("key1.pub", tempDir.resolve("key1.pub"));
    final Path key1key = copyResource("key1.key", tempDir.resolve("key1.key"));
    final Path key2pub = copyResource("key2.pub", tempDir.resolve("key2.pub"));
    final Path key2key = copyResource("key2.key", tempDir.resolve("key2.key"));

    config = NodeUtils.nodeConfig(
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
  }


  @BeforeEach
  void setUp() throws MalformedURLException {
    orionLauncher = NodeUtils.startOrion(config);
    vertx = vertx();
    orionLauncher.addPeer(new URL(NodeUtils.urlString(HOST_NAME, orionLauncher.nodePort())));
    httpClient = vertx.createHttpClient();
  }

  @AfterEach
  void tearDown() {
    orionLauncher.stop();
    vertx.close();
  }

  /** Try sending to a peer that does not exist. */
  @Test
  void missingPeer() {
    final EthClientStub orionNode = client();

    final String response = sendTransactionExpectingError(orionNode, PK_1_B_64, PK_MISSING_PEER);

    assertError(OrionErrorCode.NODE_MISSING_PEER_URL, response);
  }

  /** Try sending to a peer using a corrupted public key (wrong length). */
  @Test
  void corruptedPublicKey() {
    final EthClientStub orionNode = client();

    final String response = sendTransactionExpectingError(orionNode, PK_1_B_64, PK_CORRUPTED);

    assertError(OrionErrorCode.ENCLAVE_DECODE_PUBLIC_KEY, response);
  }

  /** Try sending using a corrupted public key (wrong `from` key). */
  @Test
  void incorrectFromAddress() {
    final EthClientStub orionNode = client();

    final String response = sendTransactionExpectingError(orionNode, PK_MISSING_PEER, PK_2_B_64);

    assertError(OrionErrorCode.ENCLAVE_NO_MATCHING_PRIVATE_KEY, response);
  }

  /** Try sending to empty privacy group. */
  @Test
  void emptyPrivacyGroup() {
    final EthClientStub orionNode = client();

    final String response = sendPrivacyGroupTransactionExpectingError(orionNode, PK_1_B_64, "");

    assertError(OrionErrorCode.ENCLAVE_PRIVACY_GROUP_MISSING, response);
  }

  /** Try sending to invalid privacy group. */
  @Test
  void invalidPrivacyGroup() {
    final EthClientStub orionNode = client();

    final String response = sendPrivacyGroupTransactionExpectingError(orionNode, PK_1_B_64, PK_CORRUPTED);

    assertError(OrionErrorCode.ENCLAVE_PRIVACY_GROUP_MISSING, response);
  }

  private EthClientStub client() {
    return NodeUtils.client(orionLauncher.clientPort(), httpClient);
  }

  /** Verifies the Orion error JSON matches the desired Orion code. */
  private void assertError(final OrionErrorCode expected, final String actual) {
    assertEquals(String.format("{\"error\":\"%s\"}", expected.code()), actual);
  }

  private String sendTransactionExpectingError(
      final EthClientStub sender,
      final String senderKey,
      final String... recipientsKey) {
    return sender.sendExpectingError(originalPayload, senderKey, recipientsKey).orElseThrow(AssertionFailedError::new);
  }

  private String sendPrivacyGroupTransactionExpectingError(
      final EthClientStub sender,
      final String senderKey,
      final String privacyGroupId) {
    return sender.sendPrivacyExpectingError(originalPayload, senderKey, privacyGroupId).orElseThrow(
        AssertionFailedError::new);
  }
}
