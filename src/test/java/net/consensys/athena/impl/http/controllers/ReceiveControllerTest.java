package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.cmd.AthenaRoutes;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.athena.impl.http.controllers.ReceiveController.ReceiveRequest;
import net.consensys.athena.impl.http.data.ContentType;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Test;

public class ReceiveControllerTest extends ControllerTest {

  @Test
  public void testPayloadIsRetrieved(TestContext context) {
    final Async async = context.async();
    // ref to storage
    final Storage storage = routes.getStorage();

    // build & serialize our payload
    SodiumEncryptedPayload encryptedPayload = mockPayload();

    // store it
    String key = storage.put(encryptedPayload);

    // and try to retrieve it with the receiveController
    ReceiveRequest req = new ReceiveRequest(key, null);

    // RECEIVE  operation, sending a ReceiveRequest
    Buffer toSend = Buffer.buffer(serializer.serialize(ContentType.JSON, req));

    vertx
        .createHttpClient()
        .post(
            httpServerPort,
            "localhost",
            AthenaRoutes.RECIEVE,
            response -> {
              context.assertEquals(200, response.statusCode());
              async.complete();
              response.handler(
                  body -> {
                    // TODO check ReceiveResponse
                    //    // read response
                    //    ReceiveResponse response = (ReceiveResponse) result.getPayload().get();
                    //
                    //    // ensure we got the decrypted response back
                    //    assertArrayEquals(Base64.decode(response.payload), toEncrypt);
                  });
            })
        .end(toSend);
  }

  @Test
  public void testResponseWhenKeyNotFound(TestContext context) throws Exception {
    final Async async = context.async();
    // and try to retrieve it with the receiveController
    ReceiveRequest req = new ReceiveRequest("notForMe", null);

    // RECEIVE  operation, sending a ReceiveRequest
    Buffer toSend = Buffer.buffer(serializer.serialize(ContentType.JSON, req));

    vertx
        .createHttpClient()
        .post(
            httpServerPort,
            "localhost",
            AthenaRoutes.RECIEVE,
            response -> {
              context.assertEquals(404, response.statusCode());
              async.complete();
            })
        .end(toSend);
  }
}
