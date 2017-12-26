package net.consensys.athena.impl.http.controllers;

import static junit.framework.TestCase.assertEquals;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.enclave.KeyStore;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.config.MemoryConfig;
import net.consensys.athena.impl.enclave.sodium.LibSodiumEnclave;
import net.consensys.athena.impl.enclave.sodium.LibSodiumSettings;
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
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import io.netty.handler.codec.http.HttpResponseStatus;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Test;

public class SendControllerTest {

  private final KeyStore memoryKeyStore = new SodiumMemoryKeyStore();
  private final MemoryConfig config = new MemoryConfig();
  private final Serializer serializer =
      new Serializer(new ObjectMapper(), new ObjectMapper(new CBORFactory()));

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

    // ensure the fakePeer got a good formatted request
    RecordedRequest recordedRequest = fakePeer.server.takeRequest();
    assertEquals("/push", recordedRequest.getPath());
    assertEquals("POST", recordedRequest.getMethod());
  }

  private SendRequest buildFakeRequest(List<FakePeer> forPeers) {
    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // create sendRequest
    PublicKey sender = memoryKeyStore.generateKeyPair();
    String from = Base64.encode(sender.getEncoded());
    String payload = Base64.encode(toEncrypt);

    String[] to =
        forPeers
            .stream()
            .map(fp -> Base64.encode(fp.publicKey.getEncoded()))
            .toArray(String[]::new);

    return new SendRequest(payload, from, to);
  }

  class FakePeer {
    final MockWebServer server;
    final PublicKey publicKey;

    public FakePeer(MockResponse response) throws IOException {
      server = new MockWebServer();
      publicKey = memoryKeyStore.generateKeyPair();
      server.enqueue(response);
      server.start();
    }

    URL getURL() {
      return server.url("").url();
    }
  }
}
