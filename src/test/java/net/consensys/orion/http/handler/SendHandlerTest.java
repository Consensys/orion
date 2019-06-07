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
package net.consensys.orion.http.handler;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.cava.crypto.Hash.sha2_512_256;
import static net.consensys.cava.io.Base64.encodeBytes;
import static net.consensys.orion.http.server.HttpContentType.APPLICATION_OCTET_STREAM;
import static net.consensys.orion.http.server.HttpContentType.CBOR;
import static net.consensys.orion.http.server.HttpContentType.JSON;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.enclave.sodium.MemoryKeyStore;
import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.http.handler.privacy.PrivacyGroupRequest;
import net.consensys.orion.http.handler.privacy.PrivacyGroups;
import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.utils.Serializer;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SendHandlerTest extends HandlerTest {

  private MemoryKeyStore memoryKeyStore;

  static {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  @BeforeEach
  void setUpKeyStore(@TempDirectory Path tempDir) {
    memoryKeyStore = new MemoryKeyStore();
  }

  @Test
  void invalidRequest() throws Exception {
    Map<String, Object> sendRequest = buildRequest(new String[] {"me"}, new byte[] {'a'}, null);

    Request request = buildPrivateAPIRequest("/send", HttpContentType.JSON, sendRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    assertEquals(500, resp.code());
  }

  @Test
  void emptyPayload() throws Exception {
    RequestBody body = RequestBody.create(null, new byte[0]);
    Request request = new Request.Builder().post(body).url(clientBaseUrl + "/send").build();

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // produces 404 because no content = no content-type = no matching with a "consumes(CBOR)"
    // route.
    assertEquals(404, resp.code());
  }

  @Test
  void sendFailsWhenBadResponseFromPeer() throws Exception {
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
  void sendFailsWhenBadDigestFromPeer() throws Exception {
    // create fake peer
    FakePeer fakePeer = new FakePeer(new MockResponse().setBody("not the best digest"));

    // add peer push URL to networkNodes
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());

    // configureRoutes our sendRequest
    Map<String, Object> sendRequest = buildRequest(Collections.singletonList(fakePeer), "foo".getBytes(UTF_8));
    Request request = buildPrivateAPIRequest("/send", HttpContentType.JSON, sendRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // ensure we got a 500 ERROR, as the fakePeer didn't return 200 OK
    assertEquals(500, resp.code());
  }

  @Test
  void sendToSinglePeer() throws Exception {
    // note: we need to do this as the fakePeers need to know in advance the digest to return.
    // not possible with libSodium due to random nonce

    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null);
    String digest = encodeBytes(sha2_512_256(encryptedPayload.cipherText()));

    // create fake peer
    FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest));
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());

    // configureRoutes our sendRequest
    Map<String, Object> sendRequest = buildRequest(Collections.singletonList(fakePeer), toEncrypt);
    Request request = buildPrivateAPIRequest("/send", HttpContentType.JSON, sendRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // ensure we got a 200 OK
    assertEquals(200, resp.code());

    // ensure peer actually got the EncryptedPayload
    RecordedRequest recordedRequest = fakePeer.server.takeRequest();

    // check method and path
    assertEquals("/push", recordedRequest.getPath());
    assertEquals("POST", recordedRequest.getMethod());

    // check header
    assertTrue(recordedRequest.getHeader("Content-Type").contains(CBOR.httpHeaderValue));

    // ensure cipher text is same.
    EncryptedPayload receivedPayload =
        Serializer.deserialize(CBOR, EncryptedPayload.class, recordedRequest.getBody().readByteArray());
    assertArrayEquals(receivedPayload.cipherText(), encryptedPayload.cipherText());
  }

  @Test
  void sendApiOnlyWorksOnPrivatePort() throws Exception {
    // note: we need to do this as the fakePeers need to know in advance the digest to return.
    // not possible with libSodium due to random nonce

    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null);
    String digest = encodeBytes(sha2_512_256(encryptedPayload.cipherText()));

    // create fake peer
    FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest));
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());

    // configureRoutes our sendRequest
    Map<String, Object> sendRequest = buildRequest(Collections.singletonList(fakePeer), toEncrypt);
    Request request = buildPublicAPIRequest("/send", HttpContentType.JSON, sendRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // ensure we got a 200 OK
    assertEquals(404, resp.code());
  }

  @Test
  void propagatedToMultiplePeers() throws Exception {
    // note: we need to do this as the fakePeers need to know in advance the digest to return.
    // not possible with libSodium due to random nonce

    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null);
    String digest = encodeBytes(sha2_512_256(encryptedPayload.cipherText()));

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
      EncryptedPayload receivedPayload =
          Serializer.deserialize(CBOR, EncryptedPayload.class, recordedRequest.getBody().readByteArray());
      assertArrayEquals(receivedPayload.cipherText(), encryptedPayload.cipherText());
    }
  }

  @Test
  void sendWithInvalidContentType() throws Exception {
    String b64String = encodeBytes("foo".getBytes(UTF_8));

    Map<String, Object> sendRequest = buildRequest(new String[] {b64String}, b64String.getBytes(UTF_8), b64String);
    // CBOR type is not available
    Request request = buildPrivateAPIRequest("/send", HttpContentType.CBOR, sendRequest);
    Response resp = httpClient.newCall(request).execute();

    // produces 404 because there is no route for the content type in the request.
    assertEquals(404, resp.code());
  }

  @Test
  void sendingWithARawBody() throws Exception {
    // note: this closely mirrors the test "testPropagatedToMultiplePeers",
    // using the raw version of the API.

    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null);
    String digest = encodeBytes(sha2_512_256(encryptedPayload.cipherText()));

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
    Box.PublicKey sender = memoryKeyStore.generateKeyPair();

    String from = encodeBytes(sender.bytesArray());

    String[] to = fakePeers.stream().map(fp -> encodeBytes(fp.publicKey.bytesArray())).toArray(String[]::new);

    Request request = new Request.Builder()
        .post(body)
        .url(clientBaseUrl + "sendraw")
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
      EncryptedPayload receivedPayload =
          Serializer.deserialize(CBOR, EncryptedPayload.class, recordedRequest.getBody().readByteArray());
      assertArrayEquals(receivedPayload.cipherText(), encryptedPayload.cipherText());
    }
  }

  @Test
  void sendRawApiOnlyWorksOnPrivatePort() throws Exception {
    // note: this closely mirrors the test "testPropagatedToMultiplePeers",
    // using the raw version of the API.

    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null);
    String digest = encodeBytes(sha2_512_256(encryptedPayload.cipherText()));

    // create fake peers
    FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest));
    // add peer push URL to networkNodes
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());

    // configureRoutes the binary sendRequest
    RequestBody body =
        RequestBody.create(MediaType.parse(HttpContentType.APPLICATION_OCTET_STREAM.httpHeaderValue), toEncrypt);
    Box.PublicKey sender = memoryKeyStore.generateKeyPair();

    String from = encodeBytes(sender.bytesArray());

    Request request = new Request.Builder()
        .post(body)
        .url(nodeBaseUrl + "sendraw")
        .addHeader("c11n-from", from)
        .addHeader("c11n-to", encodeBytes(fakePeer.publicKey.bytesArray()))
        .addHeader("Content-Type", APPLICATION_OCTET_STREAM.httpHeaderValue)
        .addHeader("Accept", APPLICATION_OCTET_STREAM.httpHeaderValue)
        .build();

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // ensure we got a 200 OK
    assertEquals(404, resp.code());
  }

  @Test
  void sendWithInvalidBody() throws Exception {
    Request requestWithInvalidBody = buildPrivateAPIRequest("/send", HttpContentType.JSON, "{\"foo\": \"bar\"}");

    Response resp = httpClient.newCall(requestWithInvalidBody).execute();

    // produces 500 because serialisation error
    assertEquals(500, resp.code());
    // checks if the failure reason was with de-serialisation
    assertError(OrionErrorCode.OBJECT_JSON_DESERIALIZATION, resp);
  }

  @Test
  void sendWithNoFrom() throws Exception {
    // note: we need to do this as the fakePeers need to know in advance the digest to return.
    // not possible with libSodium due to random nonce

    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null);
    String digest = encodeBytes(sha2_512_256(encryptedPayload.cipherText()));

    // create fake peer
    FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest));
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());

    // configureRoutes our sendRequest
    String payload = encodeBytes(toEncrypt);

    String[] to = new String[] {encodeBytes(fakePeer.publicKey.bytesArray())};

    Map<String, Object> sendRequest = buildRequest(to, payload.getBytes(UTF_8), null);
    Request request = buildPrivateAPIRequest("/send", HttpContentType.JSON, sendRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // ensure we got an error back.
    assertEquals(500, resp.code());

    assertError(OrionErrorCode.NO_SENDER_KEY, resp);
  }

  @Test
  void validPrivacyGroupIdToSinglePeer() throws Exception {
    // note: we need to do this as the fakePeers need to know in advance the digest to return.
    // not possible with libSodium due to random nonce

    // generate keys and the privacy group
    Box.PublicKey senderKey = memoryKeyStore.generateKeyPair();
    Box.PublicKey recipientKey = memoryKeyStore.generateKeyPair();
    byte[] privacyGroupId = enclave.generatePrivacyGroupId(new Box.PublicKey[] {recipientKey, senderKey});

    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, senderKey, new Box.PublicKey[] {recipientKey});
    String digest = encodeBytes(sha2_512_256(encryptedPayload.cipherText()));

    // create fake peer
    FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest));
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());

    // configureRoutes our sendRequest
    Map<String, Object> sendRequest = buildRequest(Collections.singletonList(fakePeer), toEncrypt);
    Request request = buildPrivateAPIRequest("/send", HttpContentType.JSON, sendRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // ensure we got a 200 OK
    assertEquals(200, resp.code());

    // ensure peer actually got the EncryptedPayload
    RecordedRequest recordedRequest = fakePeer.server.takeRequest();

    // check method and path
    assertEquals("/push", recordedRequest.getPath());
    assertEquals("POST", recordedRequest.getMethod());

    // check header
    assertTrue(recordedRequest.getHeader("Content-Type").contains(CBOR.httpHeaderValue));

    // ensure cipher text is same.
    EncryptedPayload receivedPayload =
        Serializer.deserialize(CBOR, EncryptedPayload.class, recordedRequest.getBody().readByteArray());
    assertArrayEquals(privacyGroupId, encryptedPayload.privacyGroupId());
    assertArrayEquals(receivedPayload.privacyGroupId(), encryptedPayload.privacyGroupId());
  }

  @Test
  void sendToPrivacyGroupId() throws Exception {
    // generate keys
    Box.PublicKey senderKey = memoryKeyStore.generateKeyPair();
    Box.PublicKey recipientKey = memoryKeyStore.generateKeyPair();

    String[] toEncrypt = new String[] {encodeBytes(senderKey.bytesArray()), encodeBytes(recipientKey.bytesArray())};
    Box.PublicKey[] addresses = Arrays.stream(toEncrypt).map(enclave::readKey).toArray(Box.PublicKey[]::new);

    // build the store privacy group request
    PrivacyGroupRequest privacyGroupRequestExpected = buildPrivacyGroupRequest(toEncrypt);
    Request request = buildPrivateAPIRequest("/privacyGroupId", JSON, privacyGroupRequestExpected);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    assertEquals(200, resp.code());

    // ensure the generated privacy group Id is same
    PrivacyGroups[] privacyGroups = Serializer.deserialize(JSON, PrivacyGroups[].class, resp.body().bytes());

    assertEquals(privacyGroups[0].getPrivacyGroupId(), encodeBytes(enclave.generatePrivacyGroupId(addresses)));

    byte[] toEncryptSend = new byte[342];
    new Random().nextBytes(toEncryptSend);

    // encrypt it here to compute digest
    EncryptedPayload encryptedPayload = enclave.encrypt(toEncryptSend, senderKey, new Box.PublicKey[] {recipientKey});
    String digest = encodeBytes(sha2_512_256(encryptedPayload.cipherText()));

    // create fake peer
    FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest));
    networkNodes.addNode(recipientKey, fakePeer.getURL());

    Map<String, Object> sendRequest = buildRequestPrivacyGroup(
        encodeBytes(encryptedPayload.privacyGroupId()),
        toEncryptSend,
        encodeBytes(senderKey.bytesArray()));

    // configureRoutes our sendRequest
    request = buildPrivateAPIRequest("/send", HttpContentType.JSON, sendRequest);

    // execute request
    resp = httpClient.newCall(request).execute();

    // ensure we got a 200 OK
    assertEquals(200, resp.code());

    // ensure peer actually got the EncryptedPayload
    RecordedRequest recordedRequest = fakePeer.server.takeRequest();

    // check method and path
    assertEquals("/push", recordedRequest.getPath());
    assertEquals("POST", recordedRequest.getMethod());

    // check header
    assertTrue(recordedRequest.getHeader("Content-Type").contains(CBOR.httpHeaderValue));

    // ensure cipher text is same.
    EncryptedPayload receivedPayload =
        Serializer.deserialize(CBOR, EncryptedPayload.class, recordedRequest.getBody().readByteArray());
    assertEquals(receivedPayload, encryptedPayload);
    assertArrayEquals(receivedPayload.privacyGroupId(), encryptedPayload.privacyGroupId());
  }

  private Map<String, Object> buildRequest(List<FakePeer> forPeers, byte[] toEncrypt) {
    Box.PublicKey sender = memoryKeyStore.generateKeyPair();
    String from = encodeBytes(sender.bytesArray());
    return buildRequest(forPeers, toEncrypt, from);
  }

  private Map<String, Object> buildRequest(List<FakePeer> forPeers, byte[] toEncrypt, String from) {
    String[] to = forPeers.stream().map(fp -> encodeBytes(fp.publicKey.bytesArray())).toArray(String[]::new);
    return buildRequest(to, toEncrypt, from);
  }

  Map<String, Object> buildRequest(String[] to, byte[] toEncrypt, String from) {
    String payload = encodeBytes(toEncrypt);

    Map<String, Object> result = new HashMap<>();
    result.put("to", to);
    result.put("payload", payload);
    if (from != null) {
      result.put("from", from);
    }
    return result;
  }

  Map<String, Object> buildRequestPrivacyGroup(String privacyGrp, byte[] toEncrypt, String from) {
    String payload = encodeBytes(toEncrypt);

    Map<String, Object> result = new HashMap<>();
    result.put("privacyGroupId", privacyGrp);
    result.put("payload", payload);
    if (from != null) {
      result.put("from", from);
    }
    return result;
  }

  PrivacyGroupRequest buildPrivacyGroupRequest(String[] addresses) {
    return new PrivacyGroupRequest(addresses);
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

    URL getURL() {
      return server.url("").url();
    }
  }
}
