package net.consensys.athena.impl.http.controllers;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.enclave.HashAlgorithm;
import net.consensys.athena.api.enclave.KeyConfig;
import net.consensys.athena.api.enclave.KeyStore;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.config.MemoryConfig;
import net.consensys.athena.impl.enclave.cesar.CesarEnclave;
import net.consensys.athena.impl.enclave.sodium.LibSodiumEnclave;
import net.consensys.athena.impl.enclave.sodium.LibSodiumSettings;
import net.consensys.athena.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.athena.impl.enclave.sodium.SodiumMemoryKeyStore;
import net.consensys.athena.impl.http.controllers.SendController.SendRequest;
import net.consensys.athena.impl.http.data.Base64;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.RequestImpl;
import net.consensys.athena.impl.http.data.Result;
import net.consensys.athena.impl.http.data.Serializer;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.network.MemoryNetworkNodes;
import net.consensys.athena.impl.storage.EncryptedPayloadStorage;
import net.consensys.athena.impl.storage.Sha512_256StorageKeyBuilder;
import net.consensys.athena.impl.storage.memory.MemoryStorage;

import java.io.IOException;
import java.net.URL;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import io.netty.handler.codec.http.HttpResponseStatus;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Test;

public class SendControllerTest {

  private final KeyConfig keyConfig = new KeyConfig("ignore", Optional.empty());;
  private final KeyStore memoryKeyStore = new SodiumMemoryKeyStore();
  private final MemoryConfig config = new MemoryConfig();
  private final Serializer serializer = new Serializer();

  // these are re-built between tests
  Enclave enclave;
  Storage<EncryptedPayload> storage;
  Controller controller;
  MemoryNetworkNodes networkNodes;

  @Before
  public void setUp() throws Exception {
    config.setLibSodiumPath(LibSodiumSettings.defaultLibSodiumPath());
    enclave = new LibSodiumEnclave(config, memoryKeyStore);

    storage =
        new EncryptedPayloadStorage(new MemoryStorage(), new Sha512_256StorageKeyBuilder(enclave));
    networkNodes = new MemoryNetworkNodes();

    controller = new SendController(enclave, storage, ContentType.JSON, networkNodes, serializer);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidRequest() {
    SendRequest sendRequest = new SendRequest(null, "me", null);
    controller.handle(new RequestImpl(sendRequest));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyPayload() {
    controller.handle(new RequestImpl());
  }

  @Test
  public void testSendFailsWhenBadResponseFromPeer() throws Exception {
    // create fake peer
    FakePeer fakePeer = new FakePeer(new MockResponse().setResponseCode(500));

    // add peer push URL to networkNodes
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());

    // build our sendRequest
    SendRequest sendRequest = buildFakeRequest(Arrays.asList(fakePeer));

    // call controller
    Result result = controller.handle(new RequestImpl(sendRequest));

    // ensure we got a 500 ERROR, as the fakePeer didn't return 200 OK
    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, result.getStatus());

    // ensure the fakePeer got a good formatted request
    RecordedRequest recordedRequest = fakePeer.server.takeRequest();
    assertEquals("/push", recordedRequest.getPath());
    assertEquals("POST", recordedRequest.getMethod());
  }

  @Test
  public void testSendFailsWhenBadDigestFromPeer() throws Exception {
    // create fake peer
    FakePeer fakePeer = new FakePeer(new MockResponse().setBody("not the best digest"));

    // add peer push URL to networkNodes
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());

    // build our sendRequest
    SendRequest sendRequest = buildFakeRequest(Arrays.asList(fakePeer));

    // call controller
    Result result = controller.handle(new RequestImpl(sendRequest));

    // ensure we got a 500 ERROR, as the digest the fakePeer sent doesn't match
    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, result.getStatus());
  }

  @Test
  public void testPropagatedToMultiplePeers() throws Exception {
    // create cesarController
    // note: we need to do this as the fakePeers need to know in advance the digest to return.
    // not possible with libSodium due to random nonce
    CesarEnclave cesarEnclave = new CesarEnclave();
    Storage cesarStorage =
        new EncryptedPayloadStorage(
            new MemoryStorage(), new Sha512_256StorageKeyBuilder(cesarEnclave));
    Controller cesarController =
        new SendController(cesarEnclave, cesarStorage, ContentType.JSON, networkNodes, serializer);

    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    EncryptedPayload encryptedPayload = cesarEnclave.encrypt(toEncrypt, null, null);
    String digest =
        Base64.encode(
            cesarEnclave.digest(HashAlgorithm.SHA_512_256, encryptedPayload.getCipherText()));

    // create fake peers
    List<FakePeer> fakePeers = new ArrayList<>(5);
    for (int i = 0; i < 5; i++) {
      FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest));
      // add peer push URL to networkNodes
      networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());
      fakePeers.add(fakePeer);
    }

    // build our sendRequest
    SendRequest sendRequest = buildFakeRequest(fakePeers, toEncrypt);

    // call controller
    Result result = cesarController.handle(new RequestImpl(sendRequest));

    // ensure we got a 200 OK
    assertEquals(HttpResponseStatus.OK, result.getStatus());

    // ensure each pear actually got the EncryptedPayload
    for (FakePeer fp : fakePeers) {
      RecordedRequest recordedRequest = fp.server.takeRequest();

      // check method and path
      assertEquals("/push", recordedRequest.getPath());
      assertEquals("POST", recordedRequest.getMethod());

      // check header
      assertTrue(
          recordedRequest.getHeader("Content-Type").contains(ContentType.CBOR.httpHeaderValue));

      // ensure cipher text is same.
      SodiumEncryptedPayload receivedPayload =
          serializer.deserialize(
              ContentType.CBOR,
              SodiumEncryptedPayload.class,
              recordedRequest.getBody().readByteArray());
      assertArrayEquals(receivedPayload.getCipherText(), encryptedPayload.getCipherText());
    }
  }

  private SendRequest buildFakeRequest(List<FakePeer> forPeers, byte[] toEncrypt) {
    // create sendRequest
    PublicKey sender = memoryKeyStore.generateKeyPair(keyConfig);
    String from = Base64.encode(sender.getEncoded());
    String payload = Base64.encode(toEncrypt);

    String[] to =
        forPeers
            .stream()
            .map(fp -> Base64.encode(fp.publicKey.getEncoded()))
            .toArray(String[]::new);

    return new SendRequest(payload, from, to);
  }

  private SendRequest buildFakeRequest(List<FakePeer> forPeers) {
    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);
    return buildFakeRequest(forPeers, toEncrypt);
  }

  class FakePeer {
    final MockWebServer server;
    final PublicKey publicKey;

    public FakePeer(MockResponse response) throws IOException {
      server = new MockWebServer();
      publicKey = memoryKeyStore.generateKeyPair(keyConfig);
      server.enqueue(response);
      server.start();
    }

    URL getURL() {
      return server.url("").url();
    }
  }
}
