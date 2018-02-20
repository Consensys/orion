
package net.consensys.athena.impl.http.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.consensys.athena.api.cmd.AthenaRoutes;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.enclave.KeyConfig;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.enclave.sodium.LibSodiumEnclave;
import net.consensys.athena.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.athena.impl.enclave.sodium.SodiumMemoryKeyStore;
import net.consensys.athena.impl.http.server.HttpContentType;

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
  public void testPayloadIsStored() throws Exception {
    // ref to storage
    final Storage storage = routes.getStorage();

    // build & serialize our payload
    EncryptedPayload encryptedPayload = mockPayload();

    // PUSH operation, sending an encrypted payload
    RequestBody body =
        RequestBody.create(
            MediaType.parse(HttpContentType.CBOR.httpHeaderValue),
            serializer.serialize(HttpContentType.CBOR, encryptedPayload));

    Request request = new Request.Builder().post(body).url(baseUrl + AthenaRoutes.PUSH).build();

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
  public void testRoundTripSerialization() {
    EncryptedPayload pushRequest = mockPayload();
    assertEquals(
        pushRequest,
        serializer.roundTrip(HttpContentType.CBOR, SodiumEncryptedPayload.class, pushRequest));
    assertEquals(
        pushRequest,
        serializer.roundTrip(HttpContentType.JSON, SodiumEncryptedPayload.class, pushRequest));
  }

  @Test
  public void testPushWithInvalidContentType() throws Exception {
    // build & serialize our payload
    EncryptedPayload encryptedPayload = mockPayload();

    // PUSH operation with invalid content type
    RequestBody body =
        RequestBody.create(
            MediaType.parse(HttpContentType.JSON.httpHeaderValue),
            serializer.serialize(HttpContentType.JSON, encryptedPayload));

    Request request = new Request.Builder().post(body).url(baseUrl + AthenaRoutes.PUSH).build();

    Response resp = httpClient.newCall(request).execute();

    assertEquals(404, resp.code());
  }

  @Test
  public void testPushWithInvalidBody() throws Exception {
    RequestBody body =
        RequestBody.create(MediaType.parse(HttpContentType.CBOR.httpHeaderValue), "foo");

    Request request = new Request.Builder().post(body).url(baseUrl + AthenaRoutes.PUSH).build();

    Response resp = httpClient.newCall(request).execute();

    // produces 500 because serialisation error
    TestCase.assertEquals(500, resp.code());
    // checks if the failure reason was with de-serialisation
    TestCase.assertTrue(resp.body().string().contains("com.fasterxml.jackson"));
  }

  protected EncryptedPayload mockPayload() {
    LibSodiumEnclave sEnclave = new LibSodiumEnclave(config, memoryKeyStore);
    PublicKey k1 = memoryKeyStore.generateKeyPair(keyConfig);
    PublicKey k2 = memoryKeyStore.generateKeyPair(keyConfig);
    return sEnclave.encrypt("something important".getBytes(), k1, new PublicKey[] {k2});
  }
}
