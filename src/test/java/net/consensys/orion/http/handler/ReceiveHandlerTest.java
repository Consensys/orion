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

import static net.consensys.cava.io.Base64.encodeBytes;
import static net.consensys.orion.http.server.HttpContentType.APPLICATION_OCTET_STREAM;
import static net.consensys.orion.http.server.HttpContentType.JSON;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.orion.enclave.Enclave;
import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.enclave.sodium.MemoryKeyStore;
import net.consensys.orion.enclave.sodium.SodiumEnclave;
import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.http.handler.receive.ReceiveRequest;
import net.consensys.orion.http.handler.receive.ReceiveResponse;
import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.storage.Storage;
import net.consensys.orion.utils.Serializer;

import java.nio.file.Path;
import java.security.Security;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

class ReceiveHandlerTest extends HandlerTest {
  private MemoryKeyStore memoryKeyStore;

  static {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  @Override
  protected Enclave buildEnclave(Path tempDir) {
    memoryKeyStore = new MemoryKeyStore();
    Box.PublicKey defaultNodeKey = memoryKeyStore.generateKeyPair();
    memoryKeyStore.addNodeKey(defaultNodeKey);
    return new SodiumEnclave(memoryKeyStore);
  }

  @SuppressWarnings("unchecked")
  @Test
  void payloadIsRetrieved() throws Exception {
    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    ReceiveRequest receiveRequest = buildReceiveRequest(payloadStorage, toEncrypt);
    Request request = buildPrivateAPIRequest("/receive", HttpContentType.JSON, receiveRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    assertEquals(200, resp.code());

    ReceiveResponse receiveResponse = Serializer.deserialize(JSON, ReceiveResponse.class, resp.body().bytes());

    assertArrayEquals(toEncrypt, receiveResponse.getPayload());
  }

  @Test
  void validPrivacyGroupId() throws Exception {
    // generate keys and the privacy group
    Box.PublicKey senderKey = memoryKeyStore.generateKeyPair();
    Box.PublicKey recipientKey = memoryKeyStore.generateKeyPair();
    byte[] privacyGroupId = getPrivacyGroupId(senderKey, recipientKey);

    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    ReceiveRequest receiveRequest =
        buildReceiveRequestSenderRecipient(payloadStorage, toEncrypt, senderKey, recipientKey);
    Request request = buildPrivateAPIRequest("/receive", HttpContentType.JSON, receiveRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    assertEquals(200, resp.code());

    ReceiveResponse receiveResponse = Serializer.deserialize(JSON, ReceiveResponse.class, resp.body().bytes());
    assertArrayEquals(toEncrypt, receiveResponse.getPayload());
    assertArrayEquals(privacyGroupId, receiveResponse.getPrivacyGroupId());
  }

  @Test
  void rawPayloadIsRetrieved() throws Exception {

    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt a payload
    Box.PublicKey senderKey = memoryKeyStore.generateKeyPair();
    EncryptedPayload originalPayload = enclave.encrypt(toEncrypt, senderKey, enclave.nodeKeys());

    // store it
    String key = payloadStorage.put(originalPayload).get();
    // Receive operation, sending a ReceivePayload request
    RequestBody body = RequestBody.create(MediaType.parse(APPLICATION_OCTET_STREAM.httpHeaderValue), "");

    Request request = new Request.Builder()
        .post(body)
        .addHeader("Content-Type", APPLICATION_OCTET_STREAM.httpHeaderValue)
        .addHeader("Accept", APPLICATION_OCTET_STREAM.httpHeaderValue)
        .addHeader("c11n-key", key)
        .url(clientBaseUrl + "receiveraw")
        .build();

    // execute request
    Response resp = httpClient.newCall(request).execute();
    assertEquals(200, resp.code());

    byte[] decodedPayload = resp.body().bytes();
    assertArrayEquals(toEncrypt, decodedPayload);
  }

  @Test
  void receiveApiOnlyWorksOnPrivatePort() throws Exception {
    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    ReceiveRequest receiveRequest = buildReceiveRequest(payloadStorage, toEncrypt);
    Request request = buildPublicAPIRequest("/receive", HttpContentType.JSON, receiveRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    assertEquals(404, resp.code());
  }

  @Test
  void receiveRawApiOnlyWorksOnPrivatePort() throws Exception {
    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt a payload
    Box.PublicKey senderKey = memoryKeyStore.generateKeyPair();
    EncryptedPayload originalPayload = enclave.encrypt(toEncrypt, senderKey, enclave.nodeKeys());

    // store it
    String key = payloadStorage.put(originalPayload).get();
    // Receive operation, sending a ReceivePayload request
    RequestBody body = RequestBody.create(MediaType.parse(APPLICATION_OCTET_STREAM.httpHeaderValue), "");

    Request request = new Request.Builder()
        .post(body)
        .addHeader("Content-Type", APPLICATION_OCTET_STREAM.httpHeaderValue)
        .addHeader("Accept", APPLICATION_OCTET_STREAM.httpHeaderValue)
        .addHeader("c11n-key", key)
        .url(nodeBaseUrl + "receiveraw")
        .build();

    // execute request
    Response resp = httpClient.newCall(request).execute();
    assertEquals(404, resp.code());
  }

  @Test
  void responseWhenKeyNotFound() throws Exception {
    // Receive operation, sending a ReceivePayload request
    ReceiveRequest receiveRequest = new ReceiveRequest("notForMe", null);

    Request request = buildPrivateAPIRequest("/receive", HttpContentType.JSON, receiveRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    assertEquals(404, resp.code());
  }

  @Test
  void responseWhenDecryptFails() throws Exception {
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    Box.PublicKey senderKey = memoryKeyStore.generateKeyPair();
    EncryptedPayload originalPayload = enclave.encrypt(toEncrypt, senderKey, new Box.PublicKey[] {senderKey});

    String key = payloadStorage.put(originalPayload).get();
    RequestBody body = RequestBody.create(MediaType.parse(APPLICATION_OCTET_STREAM.httpHeaderValue), "");

    Request request = new Request.Builder()
        .post(body)
        .addHeader("Content-Type", APPLICATION_OCTET_STREAM.httpHeaderValue)
        .addHeader("Accept", APPLICATION_OCTET_STREAM.httpHeaderValue)
        .addHeader("c11n-key", key)
        .url(clientBaseUrl + "receiveraw")
        .build();

    Response resp = httpClient.newCall(request).execute();
    assertEquals(404, resp.code());
  }

  @Test
  void roundTripSerialization() {
    Map<String, String> receiveResponse = Collections.singletonMap("payload", "some payload");
    assertEquals(receiveResponse, Serializer.roundTrip(HttpContentType.CBOR, Map.class, receiveResponse));
    assertEquals(receiveResponse, Serializer.roundTrip(HttpContentType.JSON, Map.class, receiveResponse));

    Box.PublicKey senderKey = memoryKeyStore.generateKeyPair();
    ReceiveRequest receiveRequest = new ReceiveRequest("some key", encodeBytes(senderKey.bytesArray()));
    assertEquals(receiveRequest, Serializer.roundTrip(HttpContentType.CBOR, ReceiveRequest.class, receiveRequest));
    assertEquals(receiveRequest, Serializer.roundTrip(HttpContentType.JSON, ReceiveRequest.class, receiveRequest));
  }

  @Test
  void receiveWithInvalidContentType() throws Exception {
    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // configureRoutes receive request with payload
    ReceiveRequest receiveRequest = buildReceiveRequest(payloadStorage, toEncrypt);
    Request request = buildPrivateAPIRequest("/receive", HttpContentType.CBOR, receiveRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    assertEquals(404, resp.code());
  }

  @Test
  void receiveWithInvalidBody() throws Exception {
    Request request = buildPrivateAPIRequest("/receive", HttpContentType.JSON, "{\"foo\": \"bar\"}");

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // produces 500 because serialisation error
    assertEquals(500, resp.code());
    // checks if the failure reason was with de-serialisation
    assertError(OrionErrorCode.OBJECT_JSON_DESERIALIZATION, resp);
  }

  private ReceiveRequest buildReceiveRequest(Storage<EncryptedPayload> storage, byte[] toEncrypt) throws Exception {
    // encrypt a payload
    Box.PublicKey senderKey = memoryKeyStore.generateKeyPair();
    Box.PublicKey recipientKey = memoryKeyStore.generateKeyPair();

    return buildReceiveRequestSenderRecipient(storage, toEncrypt, senderKey, recipientKey);
  }

  private ReceiveRequest buildReceiveRequestSenderRecipient(
      Storage<EncryptedPayload> storage,
      byte[] toEncrypt,
      Box.PublicKey senderKey,
      Box.PublicKey recipientKey) throws Exception {
    // encrypt a payload
    EncryptedPayload originalPayload = enclave.encrypt(toEncrypt, senderKey, new Box.PublicKey[] {recipientKey});

    // store it
    String key = storage.put(originalPayload).get();

    // Receive operation, sending a ReceivePayload request
    return new ReceiveRequest(key, encodeBytes(recipientKey.bytesArray()));
  }

  byte[] getPrivacyGroupId(Box.PublicKey sender, Box.PublicKey recipient) {
    Box.PublicKey[] tempArray = new Box.PublicKey[] {sender, recipient};
    return enclave.generatePrivacyGroupId(tempArray);
  }
}
