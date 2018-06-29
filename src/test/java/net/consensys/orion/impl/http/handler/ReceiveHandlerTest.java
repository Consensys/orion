package net.consensys.orion.impl.http.handler;

import static net.consensys.orion.impl.http.server.HttpContentType.APPLICATION_OCTET_STREAM;
import static net.consensys.orion.impl.http.server.HttpContentType.JSON;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.enclave.KeyConfig;
import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.api.storage.Storage;
import net.consensys.orion.impl.enclave.sodium.LibSodiumEnclave;
import net.consensys.orion.impl.enclave.sodium.SodiumMemoryKeyStore;
import net.consensys.orion.impl.enclave.sodium.SodiumPublicKey;
import net.consensys.orion.impl.http.handler.receive.ReceiveRequest;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Base64;
import net.consensys.orion.impl.utils.Serializer;

import java.nio.file.Path;
import java.security.PublicKey;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

class ReceiveHandlerTest extends HandlerTest {
  private KeyConfig keyConfig;
  private SodiumMemoryKeyStore memoryKeyStore;

  @Override
  protected Enclave buildEnclave(Path tempDir) {
    keyConfig = new KeyConfig(tempDir.resolve("ignore"), Optional.empty());
    memoryKeyStore = new SodiumMemoryKeyStore();
    SodiumPublicKey defaultNodeKey = (SodiumPublicKey) memoryKeyStore.generateKeyPair(keyConfig);
    memoryKeyStore.addNodeKey(defaultNodeKey);
    return new LibSodiumEnclave(memoryKeyStore);
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

    final Map<String, String> receiveResponse = Serializer.deserialize(JSON, Map.class, resp.body().bytes());

    byte[] decodedPayload = Base64.decode(receiveResponse.get("payload"));
    assertArrayEquals(toEncrypt, decodedPayload);
  }

  @Test
  void rawPayloadIsRetrieved() throws Exception {

    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt a payload
    SodiumPublicKey senderKey = (SodiumPublicKey) memoryKeyStore.generateKeyPair(keyConfig);
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
    SodiumPublicKey senderKey = (SodiumPublicKey) memoryKeyStore.generateKeyPair(keyConfig);
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

    SodiumPublicKey senderKey = (SodiumPublicKey) memoryKeyStore.generateKeyPair(keyConfig);
    EncryptedPayload originalPayload = enclave.encrypt(toEncrypt, senderKey, new PublicKey[] {senderKey});

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

    SodiumPublicKey senderKey = (SodiumPublicKey) memoryKeyStore.generateKeyPair(keyConfig);
    ReceiveRequest receiveRequest = new ReceiveRequest("some key", senderKey.toString());
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
    SodiumPublicKey senderKey = (SodiumPublicKey) memoryKeyStore.generateKeyPair(keyConfig);
    SodiumPublicKey recipientKey = (SodiumPublicKey) memoryKeyStore.generateKeyPair(keyConfig);
    EncryptedPayload originalPayload = enclave.encrypt(toEncrypt, senderKey, new PublicKey[] {recipientKey});

    // store it
    String key = storage.put(originalPayload).get();

    // Receive operation, sending a ReceivePayload request
    return new ReceiveRequest(key, recipientKey.toString());
  }
}
