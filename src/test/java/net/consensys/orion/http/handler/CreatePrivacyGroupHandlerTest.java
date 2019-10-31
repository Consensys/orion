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
import java.util.Collections;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

public class CreatePrivacyGroupHandlerTest extends HandlerTest {
  private MemoryKeyStore memoryKeyStore;

  @Override
  protected Enclave buildEnclave(final Path tempDir) {
    memoryKeyStore = new MemoryKeyStore();
    final Box.PublicKey defaultNodeKey = memoryKeyStore.generateKeyPair();
    memoryKeyStore.addNodeKey(defaultNodeKey);
    return new SodiumEnclave(memoryKeyStore);
  }

  @Test
  void expectedPrivacyGroupId() throws Exception {
    final Box.PublicKey senderKey = memoryKeyStore.generateKeyPair();
    final Box.PublicKey recipientKey = memoryKeyStore.generateKeyPair();

    final String[] toEncrypt =
        new String[] {encodeBytes(senderKey.bytesArray()), encodeBytes(recipientKey.bytesArray())};
    final Box.PublicKey[] addresses = Arrays.stream(toEncrypt).map(enclave::readKey).toArray(Box.PublicKey[]::new);

    final PrivacyGroupRequest privacyGroupRequestExpected =
        buildPrivacyGroupRequest(toEncrypt, encodeBytes(senderKey.bytesArray()), "test", "desc");
    final Request request = buildPrivateAPIRequest("/createPrivacyGroup", JSON, privacyGroupRequestExpected);

    final byte[] privacyGroupPayload = enclave.generatePrivacyGroupId(
        addresses,
        privacyGroupRequestExpected.getSeed().get(),
        PrivacyGroupPayload.Type.PANTHEON);

    // create fake peer
    final FakePeer fakePeer = new FakePeer(new MockResponse().setBody(encodeBytes(privacyGroupPayload)), recipientKey);
    networkNodes.addNode(Collections.singletonList(fakePeer.publicKey), fakePeer.getURL());

    // execute request
    final Response resp = httpClient.newCall(request).execute();

    assertEquals(200, resp.code());


    final PrivacyGroup privacyGroup = Serializer.deserialize(JSON, PrivacyGroup.class, resp.body().bytes());

    assertEquals(privacyGroup.getPrivacyGroupId(), encodeBytes(privacyGroupPayload));
  }

  @Test
  void oddNumberOfRecipientsPrivacyGroupId() throws IOException {
    final Box.PublicKey senderKey = memoryKeyStore.generateKeyPair();
    final Box.PublicKey recipientKey1 = memoryKeyStore.generateKeyPair();
    final Box.PublicKey recipientKey2 = memoryKeyStore.generateKeyPair();
    final Box.PublicKey recipientKey3 = memoryKeyStore.generateKeyPair();

    final String[] toEncrypt = new String[] {
        encodeBytes(senderKey.bytesArray()),
        encodeBytes(recipientKey1.bytesArray()),
        encodeBytes(recipientKey2.bytesArray()),
        encodeBytes(recipientKey3.bytesArray())};

    final Box.PublicKey[] addresses = Arrays.stream(toEncrypt).map(enclave::readKey).toArray(Box.PublicKey[]::new);
    final PrivacyGroupRequest privacyGroupRequestExpected =
        buildPrivacyGroupRequest(toEncrypt, encodeBytes(senderKey.bytesArray()), "test", "desc");
    final Request request = buildPrivateAPIRequest("/createPrivacyGroup", JSON, privacyGroupRequestExpected);

    final byte[] privacyGroupPayload = enclave.generatePrivacyGroupId(
        addresses,
        privacyGroupRequestExpected.getSeed().get(),
        PrivacyGroupPayload.Type.PANTHEON);

    // create fake peers
    final FakePeer fakePeer1 =
        new FakePeer(new MockResponse().setBody(encodeBytes(privacyGroupPayload)), recipientKey1);
    networkNodes.addNode(Collections.singletonList(fakePeer1.publicKey), fakePeer1.getURL());

    final FakePeer fakePeer2 =
        new FakePeer(new MockResponse().setBody(encodeBytes(privacyGroupPayload)), recipientKey2);
    networkNodes.addNode(Collections.singletonList(fakePeer2.publicKey), fakePeer2.getURL());

    final FakePeer fakePeer3 =
        new FakePeer(new MockResponse().setBody(encodeBytes(privacyGroupPayload)), recipientKey3);
    networkNodes.addNode(Collections.singletonList(fakePeer3.publicKey), fakePeer3.getURL());

    // execute request
    final Response resp = httpClient.newCall(request).execute();

    assertEquals(200, resp.code());

    final PrivacyGroup privacyGroup = Serializer.deserialize(JSON, PrivacyGroup.class, resp.body().bytes());

    assertEquals(privacyGroup.getPrivacyGroupId(), encodeBytes(privacyGroupPayload));
  }

  @Test
  void RepeatedRecipientsPrivacyGroupId() throws IOException {
    final Box.PublicKey senderKey = memoryKeyStore.generateKeyPair();
    final Box.PublicKey recipientKey = memoryKeyStore.generateKeyPair();

    final String[] toEncrypt = new String[] {
        encodeBytes(senderKey.bytesArray()),
        encodeBytes(recipientKey.bytesArray()),
        encodeBytes(recipientKey.bytesArray()),
        encodeBytes(senderKey.bytesArray())};

    final Box.PublicKey[] addresses = Arrays.stream(toEncrypt).map(enclave::readKey).toArray(Box.PublicKey[]::new);

    final PrivacyGroupRequest privacyGroupRequestExpected =
        buildPrivacyGroupRequest(toEncrypt, encodeBytes(senderKey.bytesArray()), "test", "desc");
    final Request request = buildPrivateAPIRequest("/createPrivacyGroup", JSON, privacyGroupRequestExpected);

    final byte[] privacyGroupPayload = enclave.generatePrivacyGroupId(
        addresses,
        privacyGroupRequestExpected.getSeed().get(),
        PrivacyGroupPayload.Type.PANTHEON);

    // create fake peer
    final FakePeer fakePeer = new FakePeer(new MockResponse().setBody(encodeBytes(privacyGroupPayload)), recipientKey);
    networkNodes.addNode(Collections.singletonList(fakePeer.publicKey), fakePeer.getURL());

    // execute request
    final Response resp = httpClient.newCall(request).execute();

    assertEquals(200, resp.code());

    final PrivacyGroup privacyGroup = Serializer.deserialize(JSON, PrivacyGroup.class, resp.body().bytes());

    assertEquals(privacyGroup.getPrivacyGroupId(), encodeBytes(privacyGroupPayload));
  }

  @Test
  void ErrorIfCreateDoesntContainSelf() throws IOException {
    final Box.PublicKey senderKey = memoryKeyStore.generateKeyPair();
    final Box.PublicKey recipientKey = memoryKeyStore.generateKeyPair();

    final String[] toEncrypt = new String[] {encodeBytes(recipientKey.bytesArray())};

    final PrivacyGroupRequest privacyGroupRequestExpected =
        buildPrivacyGroupRequest(toEncrypt, encodeBytes(senderKey.bytesArray()), "test", "desc");
    final Request request = buildPrivateAPIRequest("/createPrivacyGroup", JSON, privacyGroupRequestExpected);

    // execute request
    final Response resp = httpClient.newCall(request).execute();

    assertEquals(500, resp.code());
  }

  PrivacyGroupRequest buildPrivacyGroupRequest(
      final String[] addresses,
      final String from,
      final String name,
      final String description) {
    final PrivacyGroupRequest privacyGroupRequest = new PrivacyGroupRequest(addresses, from, name, description);
    // create a random seed
    final SecureRandom random = new SecureRandom();
    final byte[] bytes = new byte[20];
    random.nextBytes(bytes);
    privacyGroupRequest.setSeed(bytes);

    return privacyGroupRequest;
  }

  @Test
  void expectedPrivacyGroupIdWithoutOptionalParameters() throws Exception {
    final Box.PublicKey senderKey = memoryKeyStore.generateKeyPair();
    final Box.PublicKey recipientKey = memoryKeyStore.generateKeyPair();

    final String[] toEncrypt =
        new String[] {encodeBytes(senderKey.bytesArray()), encodeBytes(recipientKey.bytesArray())};
    final Box.PublicKey[] addresses = Arrays.stream(toEncrypt).map(enclave::readKey).toArray(Box.PublicKey[]::new);

    final PrivacyGroupRequest privacyGroupRequestExpected =
        buildPrivacyGroupRequest(toEncrypt, encodeBytes(senderKey.bytesArray()), null, null);
    final Request request = buildPrivateAPIRequest("/createPrivacyGroup", JSON, privacyGroupRequestExpected);

    final byte[] privacyGroupPayload = enclave.generatePrivacyGroupId(
        addresses,
        privacyGroupRequestExpected.getSeed().get(),
        PrivacyGroupPayload.Type.PANTHEON);

    // create fake peer
    final FakePeer fakePeer = new FakePeer(new MockResponse().setBody(encodeBytes(privacyGroupPayload)), recipientKey);
    networkNodes.addNode(Collections.singletonList(fakePeer.publicKey), fakePeer.getURL());

    // execute request
    final Response resp = httpClient.newCall(request).execute();

    assertEquals(200, resp.code());

    final PrivacyGroup privacyGroup = Serializer.deserialize(JSON, PrivacyGroup.class, resp.body().bytes());

    assertEquals(privacyGroup.getPrivacyGroupId(), encodeBytes(privacyGroupPayload));
  }

  class FakePeer {
    final MockWebServer server;
    final Box.PublicKey publicKey;

    FakePeer(final MockResponse response) throws IOException {
      server = new MockWebServer();
      publicKey = memoryKeyStore.generateKeyPair();
      server.enqueue(response);
      server.start();
    }

    FakePeer(final MockResponse response, final Box.PublicKey givenPublicKey) throws IOException {
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
