package net.consensys.orion.impl.http.handlers;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static net.consensys.orion.impl.http.server.HttpContentType.BINARY;
import static net.consensys.orion.impl.http.server.HttpContentType.CBOR;
import static org.junit.Assert.assertArrayEquals;

import net.consensys.orion.api.cmd.AthenaRoutes;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.enclave.HashAlgorithm;
import net.consensys.orion.api.enclave.KeyConfig;
import net.consensys.orion.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.orion.impl.enclave.sodium.SodiumMemoryKeyStore;
import net.consensys.orion.impl.http.handler.send.SendRequest;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Base64;

import java.io.IOException;
import java.net.URL;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Test;

public class SendHandlerTest extends HandlerTest {

  private final KeyConfig keyConfig = new KeyConfig("ignore", Optional.empty());
  private SodiumMemoryKeyStore memoryKeyStore;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    memoryKeyStore = new SodiumMemoryKeyStore(config);
  }

  @Test
  public void testInvalidRequest() throws Exception {
    SendRequest sendRequest = new SendRequest((byte[]) null, "me", null);

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

    // produces 404 because no content = no content-type = no matching with a "consumes(CBOR)"
    // route.
    assertEquals(404, resp.code());
  }

  @Test
  public void testSendFailsWhenBadResponseFromPeer() throws Exception {
    // create fake peer
    FakePeer fakePeer = new FakePeer(new MockResponse().setResponseCode(500));

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
    FakePeer fakePeer = new FakePeer(new MockResponse().setBody("not the best digest"));

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
        Base64.encode(enclave.digest(HashAlgorithm.SHA_512_256, encryptedPayload.cipherText()));

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
      assertArrayEquals(receivedPayload.cipherText(), encryptedPayload.cipherText());
    }
  }

  @Test
  public void testSendWithInvalidContentType() throws Exception {
    String b64String = Base64.encode("foo".getBytes());

    // build our sendRequest
    SendRequest sendRequest = new SendRequest(b64String, b64String, new String[] {b64String});
    // CBOR type is not available
    Request request = buildPostRequest(AthenaRoutes.SEND, HttpContentType.CBOR, sendRequest);
    Response resp = httpClient.newCall(request).execute();

    // produces 404 because there is no route for the content type in the request.
    assertEquals(404, resp.code());
  }

  @Test
  public void testSendingWithARawBody() throws Exception {
    // note: this closely mirrors the test "testPropagatedToMultiplePeers",
    // using the raw version of the API.

    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null);
    String digest =
        Base64.encode(enclave.digest(HashAlgorithm.SHA_512_256, encryptedPayload.cipherText()));

    // create fake peers
    List<FakePeer> fakePeers = new ArrayList<>(5);
    for (int i = 0; i < 5; i++) {
      FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest));
      // add peer push URL to networkNodes
      networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());
      fakePeers.add(fakePeer);
    }

    // build the binary sendRequest
    RequestBody body =
        RequestBody.create(MediaType.parse(HttpContentType.BINARY.httpHeaderValue), toEncrypt);
    PublicKey sender = memoryKeyStore.generateKeyPair(keyConfig);

    String from = Base64.encode(sender.getEncoded());

    String[] to =
        fakePeers
            .stream()
            .map(fp -> Base64.encode(fp.publicKey.getEncoded()))
            .toArray(String[]::new);

    Request request =
        new Request.Builder()
            .post(body)
            .url(baseUrl + "sendraw")
            .addHeader("c11n-from", from)
            .addHeader("c11n-to", String.join(",", to))
            .addHeader("Content-Type", BINARY.httpHeaderValue)
            .addHeader("Accept", BINARY.httpHeaderValue)
            .build();

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // ensure we got a 200 OK
    assertEquals(200, resp.code());

    // ensure we got the right body
    assertEquals(digest, resp.body().string());

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
      assertArrayEquals(receivedPayload.cipherText(), encryptedPayload.cipherText());
    }
  }

  @Test
  public void testSendWithInvalidBody() throws Exception {
    Request requestWithInvalidBody =
        buildPostRequest(AthenaRoutes.SEND, HttpContentType.JSON, "{\"foo\": \"bar\"}");

    Response resp = httpClient.newCall(requestWithInvalidBody).execute();

    // produces 500 because serialisation error
    assertEquals(500, resp.code());
    // checks if the failure reason was with de-serialisation
    assertTrue(resp.body().string().contains("com.fasterxml.jackson"));
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
