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
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.enclave.sodium.MemoryKeyStore;
import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.helpers.FakePeer;
import net.consensys.orion.http.server.HttpContentType;

import java.nio.file.Path;
import java.security.Security;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SendHandlerTest extends HandlerTest {

  protected MemoryKeyStore memoryKeyStore;

  static {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  @BeforeEach
  void setUpKeyStore(@TempDirectory final Path tempDir) {
    memoryKeyStore = new MemoryKeyStore();
  }

  @Test
  void invalidRequest() throws Exception {
    final Map<String, Object> sendRequest = buildRequest(new String[] {"me"}, new byte[] {'a'}, null);

    final Request request = buildPrivateAPIRequest("/send", HttpContentType.JSON, sendRequest);

    // execute request
    final Response resp = httpClient.newCall(request).execute();

    assertEquals(500, resp.code());
  }

  @Test
  void emptyPayload() throws Exception {
    final RequestBody body = RequestBody.create(null, new byte[0]);
    final Request request = new Request.Builder().post(body).url(clientBaseUrl + "/send").build();

    // execute request
    final Response resp = httpClient.newCall(request).execute();

    // produces 404 because no content = no content-type = no matching with a "consumes(CBOR)"
    // route.
    assertEquals(404, resp.code());
  }

  @Test
  void sendApiOnlyWorksOnPrivatePort() throws Exception {
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

    // configureRoutes our sendRequest
    final Map<String, Object> sendRequest = buildRequest(Collections.singletonList(fakePeer), toEncrypt);
    final Request request = buildPublicAPIRequest("/send", HttpContentType.JSON, sendRequest);

    // execute request
    final Response resp = httpClient.newCall(request).execute();

    // ensure we got a 200 OK
    assertEquals(404, resp.code());
  }

  @Test
  void sendWithInvalidContentType() throws Exception {
    final String b64String = encodeBytes("foo".getBytes(UTF_8));

    final Map<String, Object> sendRequest =
        buildRequest(new String[] {b64String}, b64String.getBytes(UTF_8), b64String);
    // CBOR type is not available
    final Request request = buildPrivateAPIRequest("/send", HttpContentType.CBOR, sendRequest);
    final Response resp = httpClient.newCall(request).execute();

    // produces 404 because there is no route for the content type in the request.
    assertEquals(404, resp.code());
  }

  @Test
  void sendWithInvalidBody() throws Exception {
    final Request requestWithInvalidBody = buildPrivateAPIRequest("/send", HttpContentType.JSON, "{\"foo\": \"bar\"}");

    final Response resp = httpClient.newCall(requestWithInvalidBody).execute();

    // produces 500 because serialisation error
    assertEquals(500, resp.code());
    // checks if the failure reason was with de-serialisation
    assertError(OrionErrorCode.OBJECT_JSON_DESERIALIZATION, resp);
  }

  private Map<String, Object> buildRequest(final List<FakePeer> forPeers, final byte[] toEncrypt) {
    final Box.PublicKey sender = memoryKeyStore.generateKeyPair();
    final String from = encodeBytes(sender.bytesArray());
    return buildRequest(forPeers, toEncrypt, from);
  }

  private Map<String, Object> buildRequest(final List<FakePeer> forPeers, final byte[] toEncrypt, final String from) {
    final String[] to = forPeers.stream().map(fp -> encodeBytes(fp.publicKey.bytesArray())).toArray(String[]::new);
    return buildRequest(to, toEncrypt, from);
  }

  Map<String, Object> buildRequest(final String[] to, final byte[] toEncrypt, final String from) {
    final String payload = encodeBytes(toEncrypt);

    final Map<String, Object> result = new HashMap<>();
    result.put("to", to);
    result.put("payload", payload);
    if (from != null) {
      result.put("from", from);
    }
    return result;
  }
}
