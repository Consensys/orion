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

package net.consensys.orion.impl.http.handler;

import static net.consensys.cava.crypto.Hash.sha2_512_256;
import static net.consensys.cava.io.Base64.encodeBytes;
import static net.consensys.orion.impl.http.server.HttpContentType.CBOR;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.impl.helpers.StubEnclave;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Serializer;

import java.nio.file.Path;
import java.util.Map;
import java.util.Random;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

class SendHandlerWithNodeKeysTest extends SendHandlerTest {

  @Override
  protected Enclave buildEnclave(Path tempDir) {
    return new StubEnclave() {
      @Override
      public Box.PublicKey[] nodeKeys() {
        try {
          Box.KeyPair keyPair = Box.KeyPair.random();
          return new Box.PublicKey[] {keyPair.publicKey()};
        } catch (Throwable t) {
          throw new RuntimeException(t);
        }
      }
    };
  }

  @Test
  @Override
  void sendWithNoFrom() throws Exception {
    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt it here to compute digest
    EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null);
    String digest = encodeBytes(sha2_512_256(encryptedPayload.cipherText()));

    // create fake peer
    FakePeer fakePeer = new FakePeer(new MockResponse().setBody(digest));
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());

    String[] to = new String[] {encodeBytes(fakePeer.publicKey.bytesArray())};

    Map<String, Object> sendRequest = buildRequest(to, toEncrypt, null);
    Request request = buildPrivateAPIRequest("/send", HttpContentType.JSON, sendRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // ensure it comes back OK.
    assertEquals(200, resp.code());

    // ensure pear actually got the EncryptedPayload
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
}
