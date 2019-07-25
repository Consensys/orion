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

  public static String joinPathsAsTomlListEntry(Path... paths) {
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (Path path : paths) {
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

  public static String urlString(String host, int port) {
    return buildUrl(host, port).toString();
  }

  public static URL url(String host, int port) {
    return buildUrl(host, port).url();
  }

  public static Config nodeConfig(
      Path tempDir,
      int nodePort,
      String nodeNetworkInterface,
      int clientPort,
      String clientNetworkInterface,
      String nodeName,
      String pubKeys,
      String privKeys,
      String tls,
      String tlsServerTrust,
      String tlsClientTrust,
      String storage) throws IOException {

    Path workDir = tempDir.resolve("acceptance").toAbsolutePath();
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
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  /** It's the callers responsibility to stop the started Orion. */
  public static Orion startOrion(Config config) {
    final Orion orion = new Orion();
    orion.run(System.out, System.err, config);
    return orion;
  }

  public static EthClientStub client(int clientPort, HttpClient httpClient) {
    final EthClientStub client = new EthClientStub(clientPort, httpClient);
    assertTrue(client.upCheck());
    return client;
  }

  private static final byte[] originalPayload = "a wonderful transaction".getBytes(UTF_8);

  public static byte[] viewTransaction(EthClientStub viewer, String viewerKey, String digest) {
    return viewer.receive(digest, viewerKey).orElseThrow(AssertionFailedError::new);
  }

  public static ReceiveResponse viewTransactionPrivacyGroupId(EthClientStub viewer, String viewerKey, String digest) {
    return viewer.receivePrivacy(digest, viewerKey);
  }

  public static String sendTransaction(EthClientStub sender, String senderKey, String... recipientsKey) {
    return sender.send(originalPayload, senderKey, recipientsKey).orElseThrow(AssertionFailedError::new);
  }

  public static String sendTransactionPrivacyGroupId(EthClientStub sender, String senderKey, String privacyGroupId) {
    return sender.send(originalPayload, senderKey, privacyGroupId).orElseThrow(AssertionFailedError::new);
  }

  public static PrivacyGroup createPrivacyGroupTransaction(
      EthClientStub sender,
      String[] addresses,
      String from,
      String name,
      String description) {
    return sender.createPrivacyGroup(addresses, from, name, description).orElseThrow(AssertionFailedError::new);
  }

  public static PrivacyGroup[] findPrivacyGroupTransaction(EthClientStub sender, String[] addresses) {
    return sender.findPrivacyGroup(addresses).orElseThrow(AssertionFailedError::new);
  }

  public static String deletePrivacyGroupTransaction(EthClientStub sender, String privacyGroupId, String from) {
    return sender.deletePrivacyGroup(privacyGroupId, from).orElseThrow(AssertionFailedError::new);
  }

  /** Asserts the received payload matches that sent. */
  public static void assertTransaction(byte[] receivedPayload) {
    assertArrayEquals(originalPayload, receivedPayload);
  }
}
