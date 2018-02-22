package net.consensys.orion.impl.http.handlers;

import static junit.framework.TestCase.assertTrue;
import static net.consensys.orion.impl.http.server.HttpContentType.BINARY;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import net.consensys.orion.api.cmd.OrionRoutes;
import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.enclave.KeyConfig;
import net.consensys.orion.api.storage.Storage;
import net.consensys.orion.impl.enclave.sodium.LibSodiumEnclave;
import net.consensys.orion.impl.enclave.sodium.SodiumMemoryKeyStore;
import net.consensys.orion.impl.enclave.sodium.SodiumPublicKey;
import net.consensys.orion.impl.http.handler.receive.ReceiveRequest;
import net.consensys.orion.impl.http.handler.receive.ReceiveResponse;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Base64;

import java.security.PublicKey;
import java.util.Optional;
import java.util.Random;

import junit.framework.TestCase;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.Test;

public class ReceiveHandlerTest extends HandlerTest {
  private KeyConfig keyConfig = new KeyConfig("ignore", Optional.empty());
  private SodiumMemoryKeyStore memoryKeyStore;

  @Override
  protected Enclave buildEnclave() {
    memoryKeyStore = new SodiumMemoryKeyStore(config);
    SodiumPublicKey defaultNodeKey = (SodiumPublicKey) memoryKeyStore.generateKeyPair(keyConfig);
    memoryKeyStore.addNodeKey(defaultNodeKey);
    return new LibSodiumEnclave(config, memoryKeyStore);
  }

  @Test
  public void testPayloadIsRetrieved() throws Exception {
    // ref to storage
    final Storage storage = routes.getStorage();

    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    ReceiveRequest receiveRequest = buildReceiveRequest(storage, toEncrypt);
    Request request = buildPostRequest(OrionRoutes.RECEIVE, HttpContentType.JSON, receiveRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    assertEquals(200, resp.code());

    ReceiveResponse receiveResponse =
        serializer.deserialize(HttpContentType.JSON, ReceiveResponse.class, resp.body().bytes());

    byte[] decodedPayload = Base64.decode(receiveResponse.payload);
    assertArrayEquals(toEncrypt, decodedPayload);
  }

  @Test
  public void testRawPayloadIsRetrieved() throws Exception {
    // ref to storage
    final Storage storage = routes.getStorage();

    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // encrypt a payload
    SodiumPublicKey senderKey = (SodiumPublicKey) memoryKeyStore.generateKeyPair(keyConfig);
    EncryptedPayload originalPayload = enclave.encrypt(toEncrypt, senderKey, enclave.nodeKeys());

    // store it
    String key = storage.put(originalPayload);
    // Receive operation, sending a ReceivePayload request
    RequestBody body = RequestBody.create(MediaType.parse(BINARY.httpHeaderValue), "");

    Request request =
        new Request.Builder()
            .post(body)
            .addHeader("Content-Type", BINARY.httpHeaderValue)
            .addHeader("Accept", BINARY.httpHeaderValue)
            .addHeader("c11n-key", key)
            .url(baseUrl + "receiveraw")
            .build();

    // execute request
    Response resp = httpClient.newCall(request).execute();
    assertEquals(200, resp.code());

    byte[] decodedPayload = resp.body().bytes();
    assertArrayEquals(toEncrypt, decodedPayload);
  }

  @Test
  public void testResponseWhenKeyNotFound() throws Exception {
    // Receive operation, sending a ReceivePayload request
    ReceiveRequest receiveRequest = new ReceiveRequest("notForMe", null);

    Request request = buildPostRequest(OrionRoutes.RECEIVE, HttpContentType.JSON, receiveRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    assertEquals(404, resp.code());
  }

  @Test
  public void testRoundTripSerialization() {
    ReceiveResponse receiveResponse = new ReceiveResponse("some payload");
    assertEquals(
        receiveResponse,
        serializer.roundTrip(HttpContentType.CBOR, ReceiveResponse.class, receiveResponse));
    assertEquals(
        receiveResponse,
        serializer.roundTrip(HttpContentType.JSON, ReceiveResponse.class, receiveResponse));

    SodiumPublicKey senderKey = (SodiumPublicKey) memoryKeyStore.generateKeyPair(keyConfig);
    ReceiveRequest receiveRequest = new ReceiveRequest("some key", senderKey.toString());
    assertEquals(
        receiveRequest,
        serializer.roundTrip(HttpContentType.CBOR, ReceiveRequest.class, receiveRequest));
    assertEquals(
        receiveRequest,
        serializer.roundTrip(HttpContentType.JSON, ReceiveRequest.class, receiveRequest));
  }

  @Test
  public void testReceiveWithInvalidContentType() throws Exception {
    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // build receive request with payload
    ReceiveRequest receiveRequest = buildReceiveRequest(routes.getStorage(), toEncrypt);
    Request request = buildPostRequest(OrionRoutes.RECEIVE, HttpContentType.CBOR, receiveRequest);

    // execute request
    Response resp = httpClient.newCall(request).execute();

    assertEquals(404, resp.code());
  }

  @Test
  public void testReceiveWithInvalidBody() throws Exception {
    Request request =
        buildPostRequest(OrionRoutes.RECEIVE, HttpContentType.JSON, "{\"foo\": \"bar\"}");

    // execute request
    Response resp = httpClient.newCall(request).execute();

    // produces 500 because serialisation error
    TestCase.assertEquals(500, resp.code());
    // checks if the failure reason was with de-serialisation
    assertTrue(resp.body().string().contains("com.fasterxml.jackson"));
  }

  private ReceiveRequest buildReceiveRequest(Storage storage, byte[] toEncrypt) {
    // encrypt a payload
    SodiumPublicKey senderKey = (SodiumPublicKey) memoryKeyStore.generateKeyPair(keyConfig);
    SodiumPublicKey recipientKey = (SodiumPublicKey) memoryKeyStore.generateKeyPair(keyConfig);
    EncryptedPayload originalPayload =
        enclave.encrypt(toEncrypt, senderKey, new PublicKey[] {recipientKey});

    // store it
    String key = storage.put(originalPayload);

    // Receive operation, sending a ReceivePayload request
    return new ReceiveRequest(key, recipientKey.toString());
  }
}
