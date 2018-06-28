
package net.consensys.orion.impl.http.handler;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.enclave.KeyConfig;
import net.consensys.orion.api.enclave.PublicKey;
import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.impl.enclave.sodium.LibSodiumEnclave;
import net.consensys.orion.impl.enclave.sodium.MemoryKeyStore;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Serializer;

import java.nio.file.Path;
import java.util.Optional;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PushHandlerTest extends HandlerTest {

  private KeyConfig keyConfig;
  private MemoryKeyStore memoryKeyStore;

  @BeforeEach
  void setUpKeyStore(@TempDirectory Path tempDir) {
    keyConfig = new KeyConfig(tempDir.resolve("ignore"), Optional.empty());
    memoryKeyStore = new MemoryKeyStore();
  }

  @Test
  void payloadIsStored() throws Exception {
    // configureRoutes & serialize our payload
    EncryptedPayload encryptedPayload = mockPayload();

    // PUSH operation, sending an encrypted payload
    RequestBody body = RequestBody.create(
        MediaType.parse(HttpContentType.CBOR.httpHeaderValue),
        Serializer.serialize(HttpContentType.CBOR, encryptedPayload));

    Request request = new Request.Builder().post(body).url(nodeBaseUrl + "/push").build();

    Response resp = httpClient.newCall(request).execute();

    assertEquals(200, resp.code());
    String digest = resp.body().string();
    assertTrue(digest.length() > 0);

    // we should be able to read that from storage
    Optional<EncryptedPayload> data = payloadStorage.get(digest).get();
    assertTrue(data.isPresent());
    assertEquals(encryptedPayload, data.get());
  }

  @Test
  void roundTripSerialization() {
    EncryptedPayload pushRequest = mockPayload();
    assertEquals(pushRequest, Serializer.roundTrip(HttpContentType.CBOR, EncryptedPayload.class, pushRequest));
    assertEquals(pushRequest, Serializer.roundTrip(HttpContentType.JSON, EncryptedPayload.class, pushRequest));
  }

  @Test
  void pushWithInvalidContentType() throws Exception {
    // configureRoutes & serialize our payload
    EncryptedPayload encryptedPayload = mockPayload();

    // PUSH operation with invalid content type
    RequestBody body = RequestBody.create(
        MediaType.parse(HttpContentType.JSON.httpHeaderValue),
        Serializer.serialize(HttpContentType.JSON, encryptedPayload));

    Request request = new Request.Builder().post(body).url(nodeBaseUrl + "/push").build();

    Response resp = httpClient.newCall(request).execute();

    assertEquals(404, resp.code());
  }

  @Test
  void pushWithInvalidBody() throws Exception {
    RequestBody body = RequestBody.create(MediaType.parse(HttpContentType.CBOR.httpHeaderValue), "foo");

    Request request = new Request.Builder().post(body).url(nodeBaseUrl + "/push").build();

    Response resp = httpClient.newCall(request).execute();

    // produces 500 because serialisation error
    assertEquals(500, resp.code());
    // checks if the failure reason was with de-serialisation
    assertError(OrionErrorCode.OBJECT_JSON_DESERIALIZATION, resp);
  }

  private EncryptedPayload mockPayload() {
    LibSodiumEnclave sEnclave = new LibSodiumEnclave(memoryKeyStore);
    PublicKey k1 = memoryKeyStore.generateKeyPair(keyConfig);
    PublicKey k2 = memoryKeyStore.generateKeyPair(keyConfig);
    return sEnclave.encrypt("something important".getBytes(UTF_8), k1, new PublicKey[] {k2});
  }
}
