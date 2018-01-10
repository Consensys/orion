
package net.consensys.athena.impl.http.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.consensys.athena.api.cmd.AthenaRoutes;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.athena.impl.http.data.ContentType;

import java.util.Optional;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.Test;

public class PushHandlerTest extends HandlerTest {

  @Test
  public void testPayloadIsStored() throws Exception {
    // ref to storage
    final Storage storage = routes.getStorage();

    // build & serialize our payload
    SodiumEncryptedPayload encryptedPayload = mockPayload();

    // PUSH operation, sending an encrypted payload
    RequestBody body =
        RequestBody.create(
            MediaType.parse(ContentType.CBOR.httpHeaderValue),
            serializer.serialize(ContentType.CBOR, encryptedPayload));

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
    SodiumEncryptedPayload pushRequest = mockPayload();
    assertEquals(
        pushRequest,
        serializer.roundTrip(ContentType.CBOR, SodiumEncryptedPayload.class, pushRequest));
    assertEquals(
        pushRequest,
        serializer.roundTrip(ContentType.JSON, SodiumEncryptedPayload.class, pushRequest));
  }
}
