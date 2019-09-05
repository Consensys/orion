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
package net.consensys.orion.acceptance;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.orion.cmd.Orion;
import net.consensys.orion.config.Config;
import net.consensys.orion.http.handler.privacy.PrivacyGroup;
import net.consensys.orion.http.handler.receive.ReceiveResponse;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import io.vertx.core.http.HttpClient;
import junit.framework.AssertionFailedError;
import okhttp3.HttpUrl;

public class NodeUtils {

  public static String joinPathsAsTomlListEntry(final Path... paths) {
    final StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (final Path path : paths) {
      if (!first) {
        builder.append(",");
      }
      first = false;
      builder.append("\"").append(path.toAbsolutePath().toString()).append("\"");
    }
    return builder.toString();
  }

  private static HttpUrl buildUrl(final String host, final int port) {
    return new HttpUrl.Builder().scheme("http").host(host).port(port).build();
  }

  public static String urlString(final String host, final int port) {
    return buildUrl(host, port).toString();
  }

  public static URL url(final String host, final int port) {
    return buildUrl(host, port).url();
  }

  public static Config nodeConfig(
      final Path tempDir,
      final int nodePort,
      final String nodeNetworkInterface,
      final int clientPort,
      final String clientNetworkInterface,
      final String nodeName,
      final String pubKeys,
      final String privKeys,
      final String tls,
      final String tlsServerTrust,
      final String tlsClientTrust,
      final String storage) throws IOException {

    final Path workDir = tempDir.resolve("acceptance").toAbsolutePath();
    Files.createDirectories(workDir);

    // @formatter:off
    final String confString =
          "tls=\"" + tls + "\"\n"
        + "tlsservertrust=\"" + tlsServerTrust + "\"\n"
        + "tlsclienttrust=\"" + tlsClientTrust + "\"\n"
        + "nodeport = " + nodePort + "\n"
        + "nodenetworkinterface = \"" + nodeNetworkInterface + "\"\n"
        + "clientport = " + clientPort + "\n"
        + "clientnetworkinterface = \"" + clientNetworkInterface + "\"\n"
        + "storage = \"" + storage + "\"\n"
        + "publickeys = [" + pubKeys + "]\n"
        + "privatekeys = [" + privKeys + "]\n"
        + "workdir= \"" + workDir.toString() + "\"\n";
    // @formatter:on

    return Config.load(confString);
  }

  public static int freePort() throws Exception {
    try (final ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  /** It's the callers responsibility to stop the started Orion. */
  public static Orion startOrion(final Config config) {
    final Orion orion = new Orion();
    orion.run(System.out, System.err, config);
    return orion;
  }

  public static EthClientStub client(final int clientPort, final HttpClient httpClient) {
    final EthClientStub client = new EthClientStub(clientPort, httpClient);
    assertTrue(client.upCheck());
    return client;
  }

  private static final byte[] originalPayload = "a wonderful transaction".getBytes(UTF_8);

  public static byte[] viewTransaction(final EthClientStub viewer, final String viewerKey, final String digest) {
    return viewer.receive(digest, viewerKey).orElseThrow(AssertionFailedError::new);
  }

  public static ReceiveResponse viewTransactionPrivacyGroupId(
      final EthClientStub viewer,
      final String viewerKey,
      final String digest) {
    return viewer.receivePrivacy(digest, viewerKey);
  }

  public static String sendTransaction(
      final EthClientStub sender,
      final String senderKey,
      final String... recipientsKey) {
    return sender.send(originalPayload, senderKey, recipientsKey).orElseThrow(AssertionFailedError::new);
  }

  public static String sendTransactionPrivacyGroupId(
      final EthClientStub sender,
      final String senderKey,
      final String privacyGroupId) {
    return sender.send(originalPayload, senderKey, privacyGroupId).orElseThrow(AssertionFailedError::new);
  }

  public static Boolean pushToHistory(
      final EthClientStub sender,
      final String privacyGroupId,
      final String privacyMarkerTransactionHash,
      final String enclaveKey) {
    return sender.pushToHistory(privacyGroupId, privacyMarkerTransactionHash, enclaveKey).orElseThrow(
        AssertionFailedError::new);
  }

  public static PrivacyGroup createPrivacyGroupTransaction(
      final EthClientStub sender,
      final String[] addresses,
      final String from,
      final String name,
      final String description) {
    return sender.createPrivacyGroup(addresses, from, name, description).orElseThrow(AssertionFailedError::new);
  }

  public static PrivacyGroup[] findPrivacyGroupTransaction(final EthClientStub sender, final String[] addresses) {
    return sender.findPrivacyGroup(addresses).orElseThrow(AssertionFailedError::new);
  }

  public static String deletePrivacyGroupTransaction(
      final EthClientStub sender,
      final String privacyGroupId,
      final String from) {
    return sender.deletePrivacyGroup(privacyGroupId, from).orElseThrow(AssertionFailedError::new);
  }

  /** Asserts the received payload matches that sent. */
  public static void assertTransaction(final byte[] receivedPayload) {
    assertArrayEquals(originalPayload, receivedPayload);
  }

  public static PrivacyGroup addToPrivacyGroup(
      final EthClientStub sender,
      final String address,
      final String from,
      final String privacyGroupId) {
    return sender.modifyPrivacyGroup(address, from, privacyGroupId, "/addToPrivacyGroup").orElseThrow(
        AssertionFailedError::new);
  }

}
