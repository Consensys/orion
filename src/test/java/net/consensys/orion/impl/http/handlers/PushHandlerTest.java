
package net.consensys.orion.impl.http.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.consensys.orion.api.cmd.OrionRoutes;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.enclave.KeyConfig;
import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.api.storage.Storage;
import net.consensys.orion.impl.enclave.sodium.LibSodiumEnclave;
import net.consensys.orion.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.orion.impl.enclave.sodium.SodiumMemoryKeyStore;
import net.consensys.orion.impl.http.server.HttpContentType;

import java.security.PublicKey;
import java.util.Optional;

import junit.framework.TestCase;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.Before;
import org.junit.Test;

public class PushHandlerTest extends HandlerTest {

  private final KeyConfig keyConfig = new KeyConfig("ignore", Optional.empty());
  private SodiumMemoryKeyStore memoryKeyStore;

  @Before
  public void before() {
    memoryKeyStore = new SodiumMemoryKeyStore(config);
  }

  @Test
  public void payloadIsStored() throws Exception {
    // ref to storage
    final Storage storage = routes.getStorage();

    // build & serialize our payload
    EncryptedPayload encryptedPayload = mockPayload();

    // PUSH operation, sending an encrypted payload
    RequestBody body =
        RequestBody.create(
            MediaType.parse(HttpContentType.CBOR.httpHeaderValue),
            serializer.serialize(HttpContentType.CBOR, encryptedPayload));

    Request request = new Request.Builder().post(body).url(baseUrl + OrionRoutes.PUSH).build();

    Response resp = httpClient.newCall(request).execute();

    assertEquals(200, resp.code());
    String digest = resp.body().string();
    assertTrue(digest.length() > 0);

    // we should be able to read that from storage
    Optional<EncryptedPayload> data = storage.get(digest);
    assertTrue(data.isPresent());
    assertEquals(encryptedPayload, data.get());
  }

  @Test
  public void roundTripSerialization() {
    EncryptedPayload pushRequest = mockPayload();
    assertEquals(
        pushRequest,
        serializer.roundTrip(HttpContentType.CBOR, SodiumEncryptedPayload.class, pushRequest));
    assertEquals(
        pushRequest,
        serializer.roundTrip(HttpContentType.JSON, SodiumEncryptedPayload.class, pushRequest));
  }

  @Test
  public void pushWithInvalidContentType() throws Exception {
    // build & serialize our payload
    EncryptedPayload encryptedPayload = mockPayload();

    // PUSH operation with invalid content type
    RequestBody body =
        RequestBody.create(
            MediaType.parse(HttpContentType.JSON.httpHeaderValue),
            serializer.serialize(HttpContentType.JSON, encryptedPayload));

    Request request = new Request.Builder().post(body).url(baseUrl + OrionRoutes.PUSH).build();

    Response resp = httpClient.newCall(request).execute();

    assertEquals(404, resp.code());
  }

  @Test
  public void pushWithInvalidBody() throws Exception {
    RequestBody body =
        RequestBody.create(MediaType.parse(HttpContentType.CBOR.httpHeaderValue), "foo");

    Request request = new Request.Builder().post(body).url(baseUrl + OrionRoutes.PUSH).build();

    Response resp = httpClient.newCall(request).execute();

    // produces 500 because serialisation error
    TestCase.assertEquals(500, resp.code());
    // checks if the failure reason was with de-serialisation
    assertError(OrionErrorCode.OBJECT_JSON_DESERIALIZATION, resp);
  }

  private EncryptedPayload mockPayload() {
    LibSodiumEnclave sEnclave = new LibSodiumEnclave(config, memoryKeyStore);
    PublicKey k1 = memoryKeyStore.generateKeyPair(keyConfig);
    PublicKey k2 = memoryKeyStore.generateKeyPair(keyConfig);
    return sEnclave.encrypt("something important".getBytes(), k1, new PublicKey[] {k2});
  }
}
