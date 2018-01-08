
package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.cmd.AthenaRoutes;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.athena.impl.http.data.ContentType;

import java.util.Optional;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Test;

public class PushControllerTest extends ControllerTest {

  @Test
  public void testPayloadIsStored(TestContext context) {
    final Async async = context.async();
    // ref to storage
    final Storage storage = routes.getStorage();

    // build & serialize our payload
    SodiumEncryptedPayload encryptedPayload = mockPayload();

    // PUSH operation, sending an encrypted payload
    Buffer toSend = Buffer.buffer(serializer.serialize(ContentType.CBOR, encryptedPayload));

    vertx
        .createHttpClient()
        .post(
            httpServerPort,
            "localhost",
            AthenaRoutes.PUSH,
            response -> {
              context.assertEquals(200, response.statusCode());
              response.handler(
                  body -> {
                    // PUSH should return payload digest
                    String digest = body.toString();
                    context.assertTrue(digest.length() > 0);

                    // we should be able to read that from storage
                    storage.get(digest);
                    Optional<EncryptedPayload> data = storage.get(digest);

                    context.assertTrue(data.isPresent());
                    context.assertEquals(encryptedPayload, data.get());

                    async.complete();
                  });
            })
        .end(toSend);
  }
}
