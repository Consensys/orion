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
package net.consensys.orion.payload;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.cava.crypto.Hash.sha2_512_256;
import static net.consensys.cava.io.Base64.encodeBytes;
import static net.consensys.orion.http.server.HttpContentType.CBOR;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.crypto.sodium.Box.PublicKey;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.cava.kv.KeyValueStore;
import net.consensys.cava.kv.MapDBKeyValueStore;
import net.consensys.orion.config.Config;
import net.consensys.orion.enclave.Enclave;
import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.enclave.PrivacyGroupPayload;
import net.consensys.orion.enclave.PrivacyGroupPayload.State;
import net.consensys.orion.enclave.PrivacyGroupPayload.Type;
import net.consensys.orion.enclave.QueryPrivacyGroupPayload;
import net.consensys.orion.enclave.sodium.MemoryKeyStore;
import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.exception.OrionException;
import net.consensys.orion.helpers.FakePeer;
import net.consensys.orion.helpers.StubEnclave;
import net.consensys.orion.http.handler.send.SendRequest;
import net.consensys.orion.http.handler.send.SendResponse;
import net.consensys.orion.network.ConcurrentNetworkNodes;
import net.consensys.orion.storage.EncryptedPayloadStorage;
import net.consensys.orion.storage.PrivacyGroupStorage;
import net.consensys.orion.storage.QueryPrivacyGroupStorage;
import net.consensys.orion.storage.Sha512_256StorageKeyBuilder;
import net.consensys.orion.storage.Storage;
import net.consensys.orion.storage.StorageKeyBuilder;
import net.consensys.orion.utils.Serializer;

import java.nio.file.Path;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
@ExtendWith(VertxExtension.class)
class DistributePayloadManagerTest {

  private Vertx vertx;
  private Enclave enclave;
  private MemoryKeyStore memoryKeyStore;
  private Storage<EncryptedPayload> payloadStorage;
  private Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage;
  private Storage<PrivacyGroupPayload> privacyGroupStorage;
  private ConcurrentNetworkNodes networkNodes;
  private DistributePayloadManager distributePayloadManager;

  static {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  private KeyValueStore storage;

  @BeforeEach
  public void beforeEach(@TempDirectory final Path tempDir) throws Exception {
    final Config config = Config.load("tls='off'\nworkdir=\"" + tempDir + "\"");
    final Path path = tempDir.resolve("routerdb");
    final StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder();

    vertx = Vertx.vertx();
    storage = MapDBKeyValueStore.open(path);
    enclave = new StubEnclave();
    memoryKeyStore = new MemoryKeyStore();
    payloadStorage = new EncryptedPayloadStorage(storage, keyBuilder);
    privacyGroupStorage = new PrivacyGroupStorage(storage, enclave);
    queryPrivacyGroupStorage = new QueryPrivacyGroupStorage(storage, enclave);
    networkNodes = new ConcurrentNetworkNodes(config, enclave.nodeKeys());

    distributePayloadManager = new DistributePayloadManager(
        enclave,
        payloadStorage,
        privacyGroupStorage,
        queryPrivacyGroupStorage,
        networkNodes,
        vertx.createHttpClient());
  }

  @AfterEach
  void tearDown() throws Exception {
    storage.close();
    vertx.close();
  }

  @Test
  public void failsWhenBadResponseFromPeer(final VertxTestContext testContext) throws Exception {
    final FakePeer fakePeer = new FakePeer(new MockResponse().setResponseCode(500), memoryKeyStore);
    networkNodes.addNode(Collections.singletonList(fakePeer.publicKey), fakePeer.getURL());

    final SendRequest request = buildLegacyRequest(Collections.singletonList(fakePeer), "foo".getBytes(UTF_8));
    final OrionException expectedException = new OrionException(OrionErrorCode.NODE_PROPAGATING_TO_ALL_PEERS);

    distributePayloadManager.processSendRequest(request, testContext.failing(ex -> testContext.verify(() -> {
      assertThat(ex).isEqualTo(expectedException);
      testContext.completeNow();
    })));
  }

  @Test
  public void failsWhenBadDigestFromPeer(final VertxTestContext testContext) throws Exception {
    final FakePeer fakePeer = new FakePeer(new MockResponse().setBody("not the best digest"), memoryKeyStore);
    networkNodes.addNode(Collections.singletonList(fakePeer.publicKey), fakePeer.getURL());

    final SendRequest request = buildLegacyRequest(Collections.singletonList(fakePeer), "foo".getBytes(UTF_8));
    final OrionException expectedException = new OrionException(OrionErrorCode.NODE_PROPAGATING_TO_ALL_PEERS);

    distributePayloadManager.processSendRequest(request, testContext.failing(ex -> testContext.verify(() -> {
      assertThat(ex).isEqualTo(expectedException);
      testContext.completeNow();
    })));
  }

  @Test
  public void requestWithNoToOnlyStoresPayload(final VertxTestContext testContext) {
    final SendRequest request = buildLegacyRequest(Collections.emptyList(), "foo".getBytes(UTF_8));

    distributePayloadManager.processSendRequest(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertThatPayloadWasStored(response);
      testContext.completeNow();
    })));
  }

  @Test
  public void sendRequestWithNoFromFailsIfEnclaveIsEmpty(final VertxTestContext testContext) {
    final SendRequest request = buildLegacyRequest(null, Collections.emptyList(), "foo".getBytes(UTF_8));
    final OrionException expectedException = new OrionException(OrionErrorCode.NO_SENDER_KEY);

    assertThat(enclave.nodeKeys()).isEmpty();

    distributePayloadManager.processSendRequest(request, testContext.failing(ex -> testContext.verify(() -> {
      assertThat(ex).isEqualTo(expectedException);
      testContext.completeNow();
    })));
  }

  @Test
  public void sendRequestWithNoFromUsesNodeKeyIfAvailable(final VertxTestContext testContext) {
    final SendRequest request = buildLegacyRequest(null, Collections.emptyList(), "foo".getBytes(UTF_8));
    final Box.PublicKey sender = memoryKeyStore.generateKeyPair();

    // overrriding the object to create an instance with a node key available
    enclave = spy(enclave);
    when(enclave.nodeKeys()).thenReturn(new PublicKey[] {sender});
    distributePayloadManager = new DistributePayloadManager(
        enclave,
        payloadStorage,
        privacyGroupStorage,
        queryPrivacyGroupStorage,
        networkNodes,
        Vertx.vertx().createHttpClient());

    distributePayloadManager.processSendRequest(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertThatPayloadWasStored(response);
      testContext.completeNow();
    })));
  }

  @Test
  public void distributePayloadToSinglePeerUsingLegacyWay(final VertxTestContext testContext) throws Exception {
    // note: we need to do this as the fakePeers need to know in advance the digest to return.
    // not possible with libSodium due to random nonce

    // generate random byte content
    final byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    final EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null, null);
    final String digest = encodeBytes(sha2_512_256(encryptedPayload.cipherText()));

    // create fake peer
    final FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest), memoryKeyStore);
    networkNodes.addNode(Collections.singletonList(fakePeer.publicKey), fakePeer.getURL());

    final SendRequest request = buildLegacyRequest(Collections.singletonList(fakePeer), toEncrypt);

    distributePayloadManager.processSendRequest(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertThatPayloadWasStored(response);
      assertThatPushedPayloadToPeer(encryptedPayload, fakePeer);
      testContext.completeNow();
    })));
  }

  @Test
  public void distributePayloadToMultiplePeerUsingLegacyWay(final VertxTestContext testContext) throws Exception {
    // note: we need to do this as the fakePeers need to know in advance the digest to return.
    // not possible with libSodium due to random nonce

    // generate random byte content
    final byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    final EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null, null);
    final String digest = encodeBytes(sha2_512_256(encryptedPayload.cipherText()));

    final List<FakePeer> fakePeers = new ArrayList<>(5);
    for (int i = 0; i < 5; i++) {
      final FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest), memoryKeyStore);
      // add peer push URL to networkNodes
      networkNodes.addNode(Collections.singletonList(fakePeer.publicKey), fakePeer.getURL());
      fakePeers.add(fakePeer);
    }

    final SendRequest request = buildLegacyRequest(fakePeers, toEncrypt);

    distributePayloadManager.processSendRequest(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertThatPayloadWasStored(response);
      fakePeers.forEach(p -> assertThatPushedPayloadToPeer(encryptedPayload, p));
      testContext.completeNow();
    })));
  }

  @Test
  public void distributePayloadToSinglePeerUsingPrivacyGroup(final VertxTestContext testContext) throws Exception {
    // note: we need to do this as the fakePeers need to know in advance the digest to return.
    // not possible with libSodium due to random nonce

    // generate keys and the privacy group
    final Box.PublicKey senderKey = memoryKeyStore.generateKeyPair();
    final Box.PublicKey recipientKey = memoryKeyStore.generateKeyPair();

    // generate random byte content
    final byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    final EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, senderKey, null, null);
    final String digest = encodeBytes(sha2_512_256(encryptedPayload.cipherText()));

    // create fake peer
    final FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest), recipientKey);
    networkNodes.addNode(Collections.singletonList(recipientKey), fakePeer.getURL());

    final String from = encodeBytes(senderKey.bytesArray());
    final String to = encodeBytes(fakePeer.publicKey.bytesArray());

    // create privacy group
    final String privacyGroupId = privacyGroupStorage
        .put(new PrivacyGroupPayload(new String[] {from, to}, "foo", "foo", State.ACTIVE, Type.PANTHEON, new byte[0]))
        .get();

    final SendRequest request = buildPrivacyGroupRequest(from, privacyGroupId, toEncrypt);

    distributePayloadManager.processSendRequest(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertThatPayloadWasStored(response);
      assertThatPushedPayloadToPeer(encryptedPayload, fakePeer);
      testContext.completeNow();
    })));
  }

  @Test
  public void distributePayloadToMultiplePeerUsingPrivacyGroup(final VertxTestContext testContext) throws Exception {
    // note: we need to do this as the fakePeers need to know in advance the digest to return.
    // not possible with libSodium due to random nonce
    final int numOfPeers = 5;

    // generate keys and the privacy group
    final Box.PublicKey senderKey = memoryKeyStore.generateKeyPair();
    final Box.PublicKey[] recipientKeys = new Box.PublicKey[numOfPeers];
    for (int i = 0; i < numOfPeers; i++) {
      recipientKeys[i] = memoryKeyStore.generateKeyPair();
    }

    // generate random byte content
    final byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    final EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, senderKey, null, null);
    final String digest = encodeBytes(sha2_512_256(encryptedPayload.cipherText()));

    final List<FakePeer> fakePeers = new ArrayList<>(5);
    for (int i = 0; i < numOfPeers; i++) {
      // create fake peer
      final FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest), recipientKeys[i]);
      networkNodes.addNode(Collections.singletonList(recipientKeys[i]), fakePeer.getURL());
      fakePeers.add(fakePeer);
    }

    final String from = encodeBytes(senderKey.bytesArray());
    final String[] addresses = new String[numOfPeers + 1];
    addresses[0] = from;
    for (int i = 1; i < numOfPeers + 1; i++) {
      addresses[i] = encodeBytes(recipientKeys[i - 1].bytesArray());
    }

    // create privacy group
    final String privacyGroupId = privacyGroupStorage
        .put(new PrivacyGroupPayload(addresses, "foo", "foo", State.ACTIVE, Type.PANTHEON, new byte[0]))
        .get();

    final SendRequest request = buildPrivacyGroupRequest(from, privacyGroupId, toEncrypt);

    distributePayloadManager.processSendRequest(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertThatPayloadWasStored(response);
      fakePeers.forEach(p -> assertThatPushedPayloadToPeer(encryptedPayload, p));
      testContext.completeNow();
    })));
  }

  private void assertThatPayloadWasStored(final SendResponse response) {
    assertThat(response.getKey()).isNotBlank();
    payloadStorage.get(response.getKey()).handle((payload, ex) -> {
      assertThat(payload).isNotEmpty();
      return null;
    });
  }

  private void assertThatPushedPayloadToPeer(final EncryptedPayload encryptedPayload, final FakePeer fakePeer) {
    try {
      final RecordedRequest recordedRequest = fakePeer.server.takeRequest();
      assertEquals("/push", recordedRequest.getPath());
      assertEquals("POST", recordedRequest.getMethod());
      assertTrue(recordedRequest.getHeader("Content-Type").contains(CBOR.httpHeaderValue));

      // ensure cipher text is same.
      final EncryptedPayload receivedPayload =
          Serializer.deserialize(CBOR, EncryptedPayload.class, recordedRequest.getBody().readByteArray());
      assertArrayEquals(receivedPayload.cipherText(), encryptedPayload.cipherText());
    } catch (final InterruptedException e) {
      fail("Error checking if payload was pushed to peer", e);
    }
  }

  private SendRequest buildLegacyRequest(final List<FakePeer> toPeers, final byte[] payload) {
    final Box.PublicKey sender = memoryKeyStore.generateKeyPair();
    final String from = encodeBytes(sender.bytesArray());
    return buildLegacyRequest(from, toPeers, payload);
  }

  private SendRequest buildLegacyRequest(final String from, final List<FakePeer> toPeers, final byte[] payload) {
    final String[] to = toPeers.stream().map(fp -> encodeBytes(fp.publicKey.bytesArray())).toArray(String[]::new);
    return new SendRequest(payload, from, to);
  }

  private SendRequest buildPrivacyGroupRequest(final String from, final String privacyGroupId, final byte[] payload) {
    final SendRequest request = new SendRequest(payload, from, null);
    request.setPrivacyGroupId(privacyGroupId);
    return request;
  }
}
