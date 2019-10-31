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

import static net.consensys.cava.crypto.Hash.sha2_512_256;
import static net.consensys.cava.io.Base64.encodeBytes;
import static net.consensys.orion.http.server.HttpContentType.APPLICATION_OCTET_STREAM;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.enclave.sodium.MemoryKeyStore;
import net.consensys.orion.helpers.FakePeer;
import net.consensys.orion.http.server.HttpContentType;

import java.nio.file.Path;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SendRawHandlerTest extends HandlerTest {

  protected MemoryKeyStore memoryKeyStore;

  static {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  @BeforeEach
  void setUpKeyStore(@TempDirectory final Path tempDir) {
    memoryKeyStore = new MemoryKeyStore();
  }

  @Test
  void sendingWithARawBody() throws Exception {
    // note: this closely mirrors the test "testPropagatedToMultiplePeers",
    // using the raw version of the API.

    final byte[] toEncrypt = randomBytes();
    final String digest = encryptAndCalculateDigest(toEncrypt);

    // create fake peers
    final List<FakePeer> fakePeers = new ArrayList<>(5);
    for (int i = 0; i < 5; i++) {
      final FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest), memoryKeyStore);
      // add peer push URL to networkNodes
      networkNodes.addNode(Collections.singletonList(fakePeer.publicKey), fakePeer.getURL());
      fakePeers.add(fakePeer);
    }

    // configureRoutes the binary sendRequest
    final RequestBody body = RequestBody.create(MediaType.parse(APPLICATION_OCTET_STREAM.httpHeaderValue), toEncrypt);
    final Box.PublicKey sender = memoryKeyStore.generateKeyPair();

    final String from = encodeBytes(sender.bytesArray());

    final String[] to = fakePeers.stream().map(fp -> encodeBytes(fp.publicKey.bytesArray())).toArray(String[]::new);

    final Request request = new Request.Builder()
        .post(body)
        .url(clientBaseUrl + "sendraw")
        .addHeader("c11n-from", from)
        .addHeader("c11n-to", String.join(",", to))
        .addHeader("Content-Type", APPLICATION_OCTET_STREAM.httpHeaderValue)
        .addHeader("Accept", APPLICATION_OCTET_STREAM.httpHeaderValue)
        .build();

    // execute request
    final Response resp = httpClient.newCall(request).execute();

    // ensure we got a 200 OK
    assertEquals(200, resp.code());
    // ensure we got the right body
    assertEquals(digest, resp.body().string());
  }

  @Test
  void sendRawApiOnlyWorksOnPrivatePort() throws Exception {
    // note: this closely mirrors the test "testPropagatedToMultiplePeers",
    // using the raw version of the API.

    final byte[] toEncrypt = randomBytes();
    final String digest = encryptAndCalculateDigest(toEncrypt);

    // create fake peers
    final FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest), memoryKeyStore);
    // add peer push URL to networkNodes
    networkNodes.addNode(Collections.singletonList(fakePeer.publicKey), fakePeer.getURL());

    // configureRoutes the binary sendRequest
    final RequestBody body =
        RequestBody.create(MediaType.parse(HttpContentType.APPLICATION_OCTET_STREAM.httpHeaderValue), toEncrypt);
    final Box.PublicKey sender = memoryKeyStore.generateKeyPair();

    final String from = encodeBytes(sender.bytesArray());

    final Request request = new Request.Builder()
        .post(body)
        .url(nodeBaseUrl + "sendraw")
        .addHeader("c11n-from", from)
        .addHeader("c11n-to", encodeBytes(fakePeer.publicKey.bytesArray()))
        .addHeader("Content-Type", APPLICATION_OCTET_STREAM.httpHeaderValue)
        .addHeader("Accept", APPLICATION_OCTET_STREAM.httpHeaderValue)
        .build();

    final Response resp = httpClient.newCall(request).execute();

    // ensure we got a 404 Not Found
    assertEquals(404, resp.code());
  }

  @Test
  public void sendingWithoutToHeaderSucceeds() throws Exception {
    final byte[] toEncrypt = randomBytes();

    final RequestBody body =
        RequestBody.create(MediaType.parse(HttpContentType.APPLICATION_OCTET_STREAM.httpHeaderValue), toEncrypt);
    final Box.PublicKey sender = memoryKeyStore.generateKeyPair();
    final String from = encodeBytes(sender.bytesArray());

    final Request request = new Request.Builder()
        .post(body)
        .url(clientBaseUrl + "sendraw")
        .addHeader("c11n-from", from)
        .addHeader("Content-Type", APPLICATION_OCTET_STREAM.httpHeaderValue)
        .addHeader("Accept", APPLICATION_OCTET_STREAM.httpHeaderValue)
        .build();

    final Response resp = httpClient.newCall(request).execute();

    assertEquals(200, resp.code());
  }

  @NotNull
  private String encryptAndCalculateDigest(final byte[] toEncrypt) {
    final EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null, null);
    return encodeBytes(sha2_512_256(encryptedPayload.cipherText()));
  }

  private byte[] randomBytes() {
    final byte[] toEncrypt = new byte[5];
    new Random().nextBytes(toEncrypt);
    return toEncrypt;
  }
}
