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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.enclave.sodium.MemoryKeyStore;
import net.consensys.orion.enclave.sodium.SodiumEnclave;
import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.utils.Serializer;

import java.nio.file.Path;
import java.util.Optional;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PushHandlerTest extends HandlerTest {

  private MemoryKeyStore memoryKeyStore;

  @BeforeEach
  void setUpKeyStore(@TempDirectory final Path tempDir) {
    memoryKeyStore = new MemoryKeyStore();
  }

  @Test
  void payloadIsStored() throws Exception {
    // configureRoutes & serialize our payload
    final EncryptedPayload encryptedPayload = mockPayload();

    // PUSH operation, sending an encrypted payload
    final RequestBody body = RequestBody.create(
        MediaType.parse(HttpContentType.CBOR.httpHeaderValue),
        Serializer.serialize(HttpContentType.CBOR, encryptedPayload));

    final Request request = new Request.Builder().post(body).url(nodeBaseUrl + "/push").build();

    final Response resp = httpClient.newCall(request).execute();

    assertEquals(200, resp.code());
    final String digest = resp.body().string();
    assertTrue(digest.length() > 0);

    // we should be able to read that from storage
    final Optional<EncryptedPayload> data = payloadStorage.get(digest).get();
    assertTrue(data.isPresent());
    assertEquals(encryptedPayload, data.get());
  }

  @Test
  void roundTripSerialization() {
    final EncryptedPayload pushRequest = mockPayload();
    assertEquals(pushRequest, Serializer.roundTrip(HttpContentType.CBOR, EncryptedPayload.class, pushRequest));
    assertEquals(pushRequest, Serializer.roundTrip(HttpContentType.JSON, EncryptedPayload.class, pushRequest));
  }

  @Test
  void pushWithInvalidContentType() throws Exception {
    // configureRoutes & serialize our payload
    final EncryptedPayload encryptedPayload = mockPayload();

    // PUSH operation with invalid content type
    final RequestBody body = RequestBody.create(
        MediaType.parse(HttpContentType.JSON.httpHeaderValue),
        Serializer.serialize(HttpContentType.JSON, encryptedPayload));

    final Request request = new Request.Builder().post(body).url(nodeBaseUrl + "/push").build();

    final Response resp = httpClient.newCall(request).execute();

    assertEquals(404, resp.code());
  }

  @Test
  void pushWithInvalidBody() throws Exception {
    final RequestBody body = RequestBody.create(MediaType.parse(HttpContentType.CBOR.httpHeaderValue), "foo");

    final Request request = new Request.Builder().post(body).url(nodeBaseUrl + "/push").build();

    final Response resp = httpClient.newCall(request).execute();

    // produces 500 because serialisation error
    assertEquals(500, resp.code());
    // checks if the failure reason was with de-serialisation
    assertError(OrionErrorCode.OBJECT_JSON_DESERIALIZATION, resp);
  }

  private EncryptedPayload mockPayload() {
    final SodiumEnclave sEnclave = new SodiumEnclave(memoryKeyStore);
    final Box.PublicKey k1 = memoryKeyStore.generateKeyPair();
    final Box.PublicKey k2 = memoryKeyStore.generateKeyPair();
    return sEnclave.encrypt("something important".getBytes(UTF_8), k1, new Box.PublicKey[] {k2}, null);
  }
}
