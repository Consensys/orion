
package net.consensys.athena.impl.http.handlers;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static net.consensys.athena.impl.http.server.HttpContentType.CBOR;
import static org.junit.Assert.assertArrayEquals;

import net.consensys.athena.api.cmd.AthenaRoutes;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.enclave.HashAlgorithm;
import net.consensys.athena.api.enclave.KeyConfig;
import net.consensys.athena.impl.enclave.sodium.LibSodiumEnclave;
import net.consensys.athena.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.athena.impl.enclave.sodium.SodiumMemoryKeyStore;
import net.consensys.athena.impl.helpers.FakePeer;
import net.consensys.athena.impl.http.handler.send.SendRequest;
import net.consensys.athena.impl.http.server.HttpContentType;
import net.consensys.athena.impl.utils.Base64;

import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Test;

public class SendHandlerTest extends HandlerTest {

  private final KeyConfig keyConfig = new KeyConfig("ignore", Optional.empty());;
  private final SodiumMemoryKeyStore memoryKeyStore = new SodiumMemoryKeyStore();

  @Override
  @Before
  public void setUp() throws IOException {
    super.setUp();
    // dirty; needed to avoid java.lang.RuntimeException: Please set the absolute path of the libsodium libary by calling SodiumLibrary.setLibraryPath(path)
    new LibSodiumEnclave(config, memoryKeyStore);
  }

  @Test
  public void testInvalidRequest() throws Exception {
    SendRequest sendRequest = new SendRequest(null, "me", null);

    Request request = buildPostRequest(AthenaRoutes.SEND, HttpContentType.JSON, sendRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    assertEquals(500, resp.code());
  }

  @Test
  public void testEmptyPayload() throws Exception {
    RequestBody body = RequestBody.create(null, new byte[0]);
    Request request = new Request.Builder().post(body).url(baseUrl + AthenaRoutes.SEND).build();

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // produces 404 because no content = no content-type = no matching with a "consumes(CBOR)" route.
    assertEquals(404, resp.code());
  }

  @Test
  public void testSendFailsWhenBadResponseFromPeer() throws Exception {
    // create fake peer
    FakePeer fakePeer =
        new FakePeer(
            new MockResponse().setResponseCode(500), memoryKeyStore.generateKeyPair(keyConfig));

    // add peer push URL to networkNodes
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());

    // build our sendRequest
    SendRequest sendRequest = buildFakeRequest(Arrays.asList(fakePeer));

    Request request = buildPostRequest(AthenaRoutes.SEND, HttpContentType.JSON, sendRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // ensure we got a 500 ERROR, as the fakePeer didn't return 200 OK
    assertEquals(500, resp.code());
    assertEquals(
        "{\"error\":\"couldn't propagate payload to all recipients\"}", resp.body().string());

    // ensure the fakePeer got a good formatted request
    RecordedRequest recordedRequest = fakePeer.server.takeRequest();
    assertEquals(AthenaRoutes.PUSH, recordedRequest.getPath());
    assertEquals("POST", recordedRequest.getMethod());
  }

  @Test
  public void testSendFailsWhenBadDigestFromPeer() throws Exception {
    // create fake peer
    FakePeer fakePeer =
        new FakePeer(
            new MockResponse().setBody("not the best digest"),
            memoryKeyStore.generateKeyPair(keyConfig));

    // add peer push URL to networkNodes
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());

    // build our sendRequest
    SendRequest sendRequest = buildFakeRequest(Arrays.asList(fakePeer));
    Request request = buildPostRequest(AthenaRoutes.SEND, HttpContentType.JSON, sendRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // ensure we got a 500 ERROR, as the fakePeer didn't return 200 OK
    assertEquals(500, resp.code());
  }

  @Test
  public void testPropagatedToMultiplePeers() throws Exception {
    // note: we need to do this as the fakePeers need to know in advance the digest to return.
    // not possible with libSodium due to random nonce

    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null);
    String digest =
        Base64.encode(enclave.digest(HashAlgorithm.SHA_512_256, encryptedPayload.getCipherText()));

    // create fake peers
    List<FakePeer> fakePeers = new ArrayList<>(5);
    for (int i = 0; i < 5; i++) {
      FakePeer fakePeer =
          new FakePeer(
              new MockResponse().setBody(digest), memoryKeyStore.generateKeyPair(keyConfig));
      // add peer push URL to networkNodes
      networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());
      fakePeers.add(fakePeer);
    }

    // build our sendRequest
    SendRequest sendRequest = buildFakeRequest(fakePeers, toEncrypt);
    Request request = buildPostRequest(AthenaRoutes.SEND, HttpContentType.JSON, sendRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // ensure we got a 200 OK
    assertEquals(200, resp.code());

    // ensure each pear actually got the EncryptedPayload
    for (FakePeer fp : fakePeers) {
      RecordedRequest recordedRequest = fp.server.takeRequest();

      // check method and path
      assertEquals("/push", recordedRequest.getPath());
      assertEquals("POST", recordedRequest.getMethod());

      // check header
      assertTrue(recordedRequest.getHeader("Content-Type").contains(CBOR.httpHeaderValue));

      // ensure cipher text is same.
      SodiumEncryptedPayload receivedPayload =
          serializer.deserialize(
              CBOR, SodiumEncryptedPayload.class, recordedRequest.getBody().readByteArray());
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
}
