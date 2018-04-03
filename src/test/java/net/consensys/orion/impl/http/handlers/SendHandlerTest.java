package net.consensys.orion.impl.http.handlers;

import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static net.consensys.orion.impl.http.server.HttpContentType.APPLICATION_OCTET_STREAM;
import static net.consensys.orion.impl.http.server.HttpContentType.CBOR;
import static org.junit.Assert.assertArrayEquals;

import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.enclave.HashAlgorithm;
import net.consensys.orion.api.enclave.KeyConfig;
import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.orion.impl.enclave.sodium.SodiumMemoryKeyStore;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Base64;
import net.consensys.orion.impl.utils.Serializer;

import java.io.IOException;
import java.net.URL;
import java.security.PublicKey;
import java.util.*;

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

  final KeyConfig keyConfig = new KeyConfig("ignore", Optional.empty());
  SodiumMemoryKeyStore memoryKeyStore;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    memoryKeyStore = new SodiumMemoryKeyStore(config);
  }

  @Test
  public void invalidRequest() throws Exception {
    Map<String, Object> sendRequest = buildRequest(new String[] {"me"}, new byte[] {'a'}, null);

    Request request = buildPrivateAPIRequest("/send", HttpContentType.JSON, sendRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    assertEquals(500, resp.code());
  }

  @Test
  public void emptyPayload() throws Exception {
    RequestBody body = RequestBody.create(null, new byte[0]);
    Request request = new Request.Builder().post(body).url(privateBaseUrl + "/send").build();

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // produces 404 because no content = no content-type = no matching with a "consumes(CBOR)"
    // route.
    assertEquals(404, resp.code());
  }

  @Test
  public void sendFailsWhenBadResponseFromPeer() throws Exception {
    // create fake peer
    FakePeer fakePeer = new FakePeer(new MockResponse().setResponseCode(500));

    // add peer push URL to networkNodes
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());


    Map<String, Object> sendRequest = buildRequest(Collections.singletonList(fakePeer), "foo".getBytes(UTF_8));

    Request request = buildPrivateAPIRequest("/send", HttpContentType.JSON, sendRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // ensure we got a 500 ERROR, as the fakePeer didn't return 200 OK
    assertEquals(500, resp.code());
    assertError(OrionErrorCode.NODE_PROPAGATING_TO_ALL_PEERS, resp);

    // ensure the fakePeer got a good formatted request
    RecordedRequest recordedRequest = fakePeer.server.takeRequest();
    assertEquals("/push", recordedRequest.getPath());
    assertEquals("POST", recordedRequest.getMethod());
  }

  @Test
  public void sendFailsWhenBadDigestFromPeer() throws Exception {
    // create fake peer
    FakePeer fakePeer = new FakePeer(new MockResponse().setBody("not the best digest"));

    // add peer push URL to networkNodes
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());

    // configureRoutes our sendRequest
    Map<String, Object> sendRequest = buildRequest(Arrays.asList(fakePeer), "foo".getBytes(UTF_8));
    Request request = buildPrivateAPIRequest("/send", HttpContentType.JSON, sendRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // ensure we got a 500 ERROR, as the fakePeer didn't return 200 OK
    assertEquals(500, resp.code());
  }

  @Test
  public void sendToSinglePeer() throws Exception {
    // note: we need to do this as the fakePeers need to know in advance the digest to return.
    // not possible with libSodium due to random nonce

    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null);
    String digest = Base64.encode(enclave.digest(HashAlgorithm.SHA_512_256, encryptedPayload.cipherText()));

    // create fake peer
    FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest));
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());

    // configureRoutes our sendRequest
    Map<String, Object> sendRequest = buildRequest(Arrays.asList(fakePeer), toEncrypt);
    Request request = buildPrivateAPIRequest("/send", HttpContentType.JSON, sendRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // ensure we got a 200 OK
    assertEquals(200, resp.code());

    // ensure pear actually got the EncryptedPayload
    RecordedRequest recordedRequest = fakePeer.server.takeRequest();

    // check method and path
    assertEquals("/push", recordedRequest.getPath());
    assertEquals("POST", recordedRequest.getMethod());

    // check header
    assertTrue(recordedRequest.getHeader("Content-Type").contains(CBOR.httpHeaderValue));

    // ensure cipher text is same.
    SodiumEncryptedPayload receivedPayload =
        Serializer.deserialize(CBOR, SodiumEncryptedPayload.class, recordedRequest.getBody().readByteArray());
    assertArrayEquals(receivedPayload.cipherText(), encryptedPayload.cipherText());
  }

  @Test
  public void sendApiOnlyWorksOnPrivatePort() throws Exception {
    // note: we need to do this as the fakePeers need to know in advance the digest to return.
    // not possible with libSodium due to random nonce

    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null);
    String digest = Base64.encode(enclave.digest(HashAlgorithm.SHA_512_256, encryptedPayload.cipherText()));

    // create fake peer
    FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest));
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());

    // configureRoutes our sendRequest
    Map<String, Object> sendRequest = buildRequest(Arrays.asList(fakePeer), toEncrypt);
    Request request = buildPublicAPIRequest("/send", HttpContentType.JSON, sendRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // ensure we got a 200 OK
    assertEquals(404, resp.code());
  }

  @Test
  public void propagatedToMultiplePeers() throws Exception {
    // note: we need to do this as the fakePeers need to know in advance the digest to return.
    // not possible with libSodium due to random nonce

    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null);
    String digest = Base64.encode(enclave.digest(HashAlgorithm.SHA_512_256, encryptedPayload.cipherText()));

    // create fake peers
    List<FakePeer> fakePeers = new ArrayList<>(5);
    for (int i = 0; i < 5; i++) {
      FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest));
      // add peer push URL to networkNodes
      networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());
      fakePeers.add(fakePeer);
    }

    // configureRoutes our sendRequest
    Map<String, Object> sendRequest = buildRequest(fakePeers, toEncrypt);
    Request request = buildPrivateAPIRequest("/send", HttpContentType.JSON, sendRequest);

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
          Serializer.deserialize(CBOR, SodiumEncryptedPayload.class, recordedRequest.getBody().readByteArray());
      assertArrayEquals(receivedPayload.cipherText(), encryptedPayload.cipherText());
    }
  }

  @Test
  public void sendWithInvalidContentType() throws Exception {
    String b64String = Base64.encode("foo".getBytes(UTF_8));

    Map<String, Object> sendRequest = buildRequest(new String[] {b64String}, b64String.getBytes(UTF_8), b64String);
    // CBOR type is not available
    Request request = buildPrivateAPIRequest("/send", HttpContentType.CBOR, sendRequest);
    Response resp = httpClient.newCall(request).execute();

    // produces 404 because there is no route for the content type in the request.
    assertEquals(404, resp.code());
  }

  @Test
  public void sendingWithARawBody() throws Exception {
    // note: this closely mirrors the test "testPropagatedToMultiplePeers",
    // using the raw version of the API.

    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null);
    String digest = Base64.encode(enclave.digest(HashAlgorithm.SHA_512_256, encryptedPayload.cipherText()));

    // create fake peers
    List<FakePeer> fakePeers = new ArrayList<>(5);
    for (int i = 0; i < 5; i++) {
      FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest));
      // add peer push URL to networkNodes
      networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());
      fakePeers.add(fakePeer);
    }

    // configureRoutes the binary sendRequest
    RequestBody body = RequestBody.create(MediaType.parse(APPLICATION_OCTET_STREAM.httpHeaderValue), toEncrypt);
    PublicKey sender = memoryKeyStore.generateKeyPair(keyConfig);

    String from = Base64.encode(sender.getEncoded());

    String[] to = fakePeers.stream().map(fp -> Base64.encode(fp.publicKey.getEncoded())).toArray(String[]::new);

    Request request = new Request.Builder()
        .post(body)
        .url(privateBaseUrl + "sendraw")
        .addHeader("c11n-from", from)
        .addHeader("c11n-to", String.join(",", to))
        .addHeader("Content-Type", APPLICATION_OCTET_STREAM.httpHeaderValue)
        .addHeader("Accept", APPLICATION_OCTET_STREAM.httpHeaderValue)
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
          Serializer.deserialize(CBOR, SodiumEncryptedPayload.class, recordedRequest.getBody().readByteArray());
      assertArrayEquals(receivedPayload.cipherText(), encryptedPayload.cipherText());
    }
  }

  @Test
  public void sendRawApiOnlyWorksOnPrivatePort() throws Exception {
    // note: this closely mirrors the test "testPropagatedToMultiplePeers",
    // using the raw version of the API.

    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null);
    String digest = Base64.encode(enclave.digest(HashAlgorithm.SHA_512_256, encryptedPayload.cipherText()));

    // create fake peers
    FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest));
    // add peer push URL to networkNodes
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());

    // configureRoutes the binary sendRequest
    RequestBody body =
        RequestBody.create(MediaType.parse(HttpContentType.APPLICATION_OCTET_STREAM.httpHeaderValue), toEncrypt);
    PublicKey sender = memoryKeyStore.generateKeyPair(keyConfig);

    String from = Base64.encode(sender.getEncoded());

    Request request = new Request.Builder()
        .post(body)
        .url(publicBaseUrl + "sendraw")
        .addHeader("c11n-from", from)
        .addHeader("c11n-to", Base64.encode(fakePeer.publicKey.getEncoded()))
        .addHeader("Content-Type", APPLICATION_OCTET_STREAM.httpHeaderValue)
        .addHeader("Accept", APPLICATION_OCTET_STREAM.httpHeaderValue)
        .build();

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // ensure we got a 200 OK
    assertEquals(404, resp.code());
  }

  @Test
  public void sendWithInvalidBody() throws Exception {
    Request requestWithInvalidBody = buildPrivateAPIRequest("/send", HttpContentType.JSON, "{\"foo\": \"bar\"}");

    Response resp = httpClient.newCall(requestWithInvalidBody).execute();

    // produces 500 because serialisation error
    assertEquals(500, resp.code());
    // checks if the failure reason was with de-serialisation
    assertError(OrionErrorCode.OBJECT_JSON_DESERIALIZATION, resp);
  }

  @Test
  public void sendWithNoFrom() throws Exception {
    // note: we need to do this as the fakePeers need to know in advance the digest to return.
    // not possible with libSodium due to random nonce

    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null);
    String digest = Base64.encode(enclave.digest(HashAlgorithm.SHA_512_256, encryptedPayload.cipherText()));

    // create fake peer
    FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest));
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());

    // configureRoutes our sendRequest
    String payload = Base64.encode(toEncrypt);

    String[] to = new String[] {Base64.encode(fakePeer.publicKey.getEncoded())};

    Map<String, Object> sendRequest = buildRequest(to, payload.getBytes(UTF_8), null);
    Request request = buildPrivateAPIRequest("/send", HttpContentType.JSON, sendRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // ensure we got an error back.
    assertEquals(500, resp.code());

    assertError(OrionErrorCode.NO_SENDER_KEY, resp);
  }

  private Map<String, Object> buildRequest(List<FakePeer> forPeers, byte[] toEncrypt) {
    PublicKey sender = memoryKeyStore.generateKeyPair(keyConfig);
    String from = Base64.encode(sender.getEncoded());
    return buildRequest(forPeers, toEncrypt, from);
  }

  private Map<String, Object> buildRequest(List<FakePeer> forPeers, byte[] toEncrypt, String from) {
    String[] to = forPeers.stream().map(fp -> Base64.encode(fp.publicKey.getEncoded())).toArray(String[]::new);
    return buildRequest(to, toEncrypt, from);
  }

  Map<String, Object> buildRequest(String[] to, byte[] toEncrypt, String from) {
    String payload = Base64.encode(toEncrypt);

    Map<String, Object> result = new HashMap<>();
    result.put("to", to);
    result.put("payload", payload);
    if (from != null) {
      result.put("from", from);
    }
    return result;
  }

  private Map<String, Object> buildRequest(List<FakePeer> forPeers) {
    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);
    return buildRequest(forPeers, toEncrypt);
  }

  class FakePeer {
    final MockWebServer server;
    final PublicKey publicKey;

    FakePeer(MockResponse response) throws IOException {
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
