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
package net.consensys.orion.http.handler;

import static net.consensys.cava.io.Base64.encodeBytes;
import static net.consensys.orion.http.server.HttpContentType.JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.orion.enclave.Enclave;
import net.consensys.orion.enclave.PrivacyGroupPayload;
import net.consensys.orion.enclave.sodium.MemoryKeyStore;
import net.consensys.orion.enclave.sodium.SodiumEnclave;
import net.consensys.orion.http.handler.privacy.PrivacyGroup;
import net.consensys.orion.http.handler.privacy.PrivacyGroupRequest;
import net.consensys.orion.utils.Serializer;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

public class PrivacyGroupHandlerTest extends HandlerTest {
  private MemoryKeyStore memoryKeyStore;

  @Override
  protected Enclave buildEnclave(Path tempDir) {
    memoryKeyStore = new MemoryKeyStore();
    Box.PublicKey defaultNodeKey = memoryKeyStore.generateKeyPair();
    memoryKeyStore.addNodeKey(defaultNodeKey);
    return new SodiumEnclave(memoryKeyStore);
  }

  @Test
  void expectedPrivacyGroupId() throws Exception {
    Box.PublicKey senderKey = memoryKeyStore.generateKeyPair();
    Box.PublicKey recipientKey = memoryKeyStore.generateKeyPair();

    String[] toEncrypt = new String[] {encodeBytes(senderKey.bytesArray()), encodeBytes(recipientKey.bytesArray())};
    Box.PublicKey[] addresses = Arrays.stream(toEncrypt).map(enclave::readKey).toArray(Box.PublicKey[]::new);

    PrivacyGroupRequest privacyGroupRequestExpected =
        buildPrivacyGroupRequest(toEncrypt, encodeBytes(senderKey.bytesArray()), "test", "desc");
    Request request = buildPrivateAPIRequest("/privacyGroupId", JSON, privacyGroupRequestExpected);

    byte[] privacyGroupPayload = enclave.generatePrivacyGroupId(
        addresses,
        privacyGroupRequestExpected.getSeed().get(),
        PrivacyGroupPayload.Type.PANTHEON);

    // create fake peer
    FakePeer fakePeer = new FakePeer(new MockResponse().setBody(encodeBytes(privacyGroupPayload)), recipientKey);
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());

    // execute request
    Response resp = httpClient.newCall(request).execute();

    assertEquals(200, resp.code());


    PrivacyGroup privacyGroup = Serializer.deserialize(JSON, PrivacyGroup.class, resp.body().bytes());

    assertEquals(privacyGroup.getPrivacyGroupId(), encodeBytes(privacyGroupPayload));
  }

  @Test
  void oddNumberOfRecipientsPrivacyGroupId() throws IOException {
    Box.PublicKey senderKey = memoryKeyStore.generateKeyPair();
    Box.PublicKey recipientKey1 = memoryKeyStore.generateKeyPair();
    Box.PublicKey recipientKey2 = memoryKeyStore.generateKeyPair();
    Box.PublicKey recipientKey3 = memoryKeyStore.generateKeyPair();

    String[] toEncrypt = new String[] {
        encodeBytes(senderKey.bytesArray()),
        encodeBytes(recipientKey1.bytesArray()),
        encodeBytes(recipientKey2.bytesArray()),
        encodeBytes(recipientKey3.bytesArray())};

    Box.PublicKey[] addresses = Arrays.stream(toEncrypt).map(enclave::readKey).toArray(Box.PublicKey[]::new);
    PrivacyGroupRequest privacyGroupRequestExpected =
        buildPrivacyGroupRequest(toEncrypt, encodeBytes(senderKey.bytesArray()), "test", "desc");
    Request request = buildPrivateAPIRequest("/privacyGroupId", JSON, privacyGroupRequestExpected);

    byte[] privacyGroupPayload = enclave.generatePrivacyGroupId(
        addresses,
        privacyGroupRequestExpected.getSeed().get(),
        PrivacyGroupPayload.Type.PANTHEON);

    // create fake peers
    FakePeer fakePeer1 = new FakePeer(new MockResponse().setBody(encodeBytes(privacyGroupPayload)), recipientKey1);
    networkNodes.addNode(fakePeer1.publicKey, fakePeer1.getURL());

    FakePeer fakePeer2 = new FakePeer(new MockResponse().setBody(encodeBytes(privacyGroupPayload)), recipientKey2);
    networkNodes.addNode(fakePeer2.publicKey, fakePeer2.getURL());

    FakePeer fakePeer3 = new FakePeer(new MockResponse().setBody(encodeBytes(privacyGroupPayload)), recipientKey3);
    networkNodes.addNode(fakePeer3.publicKey, fakePeer3.getURL());

    // execute request
    Response resp = httpClient.newCall(request).execute();

    assertEquals(200, resp.code());

    PrivacyGroup privacyGroup = Serializer.deserialize(JSON, PrivacyGroup.class, resp.body().bytes());

    assertEquals(privacyGroup.getPrivacyGroupId(), encodeBytes(privacyGroupPayload));
  }

  @Test
  void RepeatedRecipientsPrivacyGroupId() throws IOException {
    Box.PublicKey senderKey = memoryKeyStore.generateKeyPair();
    Box.PublicKey recipientKey = memoryKeyStore.generateKeyPair();

    String[] toEncrypt = new String[] {
        encodeBytes(senderKey.bytesArray()),
        encodeBytes(recipientKey.bytesArray()),
        encodeBytes(recipientKey.bytesArray()),
        encodeBytes(senderKey.bytesArray())};

    Box.PublicKey[] addresses = Arrays.stream(toEncrypt).map(enclave::readKey).toArray(Box.PublicKey[]::new);

    PrivacyGroupRequest privacyGroupRequestExpected =
        buildPrivacyGroupRequest(toEncrypt, encodeBytes(senderKey.bytesArray()), "test", "desc");
    Request request = buildPrivateAPIRequest("/privacyGroupId", JSON, privacyGroupRequestExpected);

    byte[] privacyGroupPayload = enclave.generatePrivacyGroupId(
        addresses,
        privacyGroupRequestExpected.getSeed().get(),
        PrivacyGroupPayload.Type.PANTHEON);

    // create fake peer
    FakePeer fakePeer = new FakePeer(new MockResponse().setBody(encodeBytes(privacyGroupPayload)), recipientKey);
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());

    // execute request
    Response resp = httpClient.newCall(request).execute();

    assertEquals(200, resp.code());

    PrivacyGroup privacyGroup = Serializer.deserialize(JSON, PrivacyGroup.class, resp.body().bytes());

    assertEquals(privacyGroup.getPrivacyGroupId(), encodeBytes(privacyGroupPayload));
  }

  PrivacyGroupRequest buildPrivacyGroupRequest(String[] addresses, String from, String name, String description) {
    PrivacyGroupRequest privacyGroupRequest = new PrivacyGroupRequest(addresses, from, name, description);
    // create a random seed
    SecureRandom random = new SecureRandom();
    byte[] bytes = new byte[20];
    random.nextBytes(bytes);
    privacyGroupRequest.setSeed(bytes);

    return privacyGroupRequest;
  }

  class FakePeer {
    final MockWebServer server;
    final Box.PublicKey publicKey;

    FakePeer(MockResponse response) throws IOException {
      server = new MockWebServer();
      publicKey = memoryKeyStore.generateKeyPair();
      server.enqueue(response);
      server.start();
    }

    FakePeer(MockResponse response, Box.PublicKey givenPublicKey) throws IOException {
      server = new MockWebServer();
      publicKey = givenPublicKey;
      server.enqueue(response);
      server.start();
    }

    URL getURL() {
      return server.url("").url();
    }
  }
}
