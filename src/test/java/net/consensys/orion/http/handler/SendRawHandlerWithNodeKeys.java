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
import net.consensys.orion.enclave.Enclave;
import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.helpers.FakePeer;
import net.consensys.orion.helpers.StubEnclave;
import net.consensys.orion.http.server.HttpContentType;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Test;

public class SendRawHandlerWithNodeKeys extends SendRawHandlerTest {

  @Override
  protected Enclave buildEnclave(final Path tempDir) {
    return new StubEnclave() {
      @Override
      public Box.PublicKey[] nodeKeys() {
        try {
          final Box.KeyPair keyPair = Box.KeyPair.random();
          return new Box.PublicKey[] {keyPair.publicKey()};
        } catch (final Throwable t) {
          throw new RuntimeException(t);
        }
      }
    };
  }

  @Test
  public void sendingWithoutFromHeaderSucceeds() throws Exception {
    final byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    final RequestBody body =
        RequestBody.create(MediaType.parse(HttpContentType.APPLICATION_OCTET_STREAM.httpHeaderValue), toEncrypt);

    final EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null, null);
    final String digest = encodeBytes(sha2_512_256(encryptedPayload.cipherText()));

    final FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest), memoryKeyStore);
    networkNodes.addNode(Collections.singletonList(fakePeer.publicKey), fakePeer.getURL());
    final String[] to = new String[] {encodeBytes(fakePeer.publicKey.bytesArray())};

    final Request request = new Request.Builder()
        .post(body)
        .url(clientBaseUrl + "sendraw")
        .addHeader("c11n-to", String.join(",", to))
        .addHeader("Content-Type", APPLICATION_OCTET_STREAM.httpHeaderValue)
        .addHeader("Accept", APPLICATION_OCTET_STREAM.httpHeaderValue)
        .build();

    final Response resp = httpClient.newCall(request).execute();

    assertEquals(200, resp.code());
  }

}
