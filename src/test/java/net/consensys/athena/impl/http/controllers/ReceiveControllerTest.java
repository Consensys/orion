package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.cmd.AthenaRoutes;

import java.util.Random;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Test;

public class ReceiveControllerTest extends ControllerTest {

  @Test
  public void testPayloadIsRetrieved(TestContext context) {
    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    final Async async = context.async();
    vertx
        .createHttpClient()
        .getNow(
            httpServerPort,
            "localhost",
            AthenaRoutes.UPCHECK,
            response -> {
              response.handler(
                  body -> {
                    context.assertEquals("I'm up!", body.toString());
                    async.complete();
                  });
            });

    //    // generate dummy encrypted payload object
    //    EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null);
    //
    //    // store it
    //    String key = storage.put(encryptedPayload);
    //
    //    // and try to retrieve it with the receiveController
    //    ReceiveRequest req = new ReceiveRequest(key, null);
    //
    //    // submit request to controller
    //    Request controllerRequest = new RequestImpl(req);
    //    Result result = receiveController.handle(controllerRequest);
    //
    //    // ensure we got a 200 OK back
    //    assertEquals(result.getStatus().code(), HttpResponseStatus.OK.code());
    //
    //    // ensure result has a payload
    //    assert (result.getPayload().isPresent());
    //
    //    // read response
    //    ReceiveResponse response = (ReceiveResponse) result.getPayload().get();
    //
    //    // ensure we got the decrypted response back
    //    assertArrayEquals(Base64.decode(response.payload), toEncrypt);
  }

  @Test
  public void testResponseWhenKeyNotFound() throws Exception {
    //    ReceiveRequest req = new ReceiveRequest("notForMe", null);
    //
    //    Result result = receiveController.handle(new RequestImpl(req));
    //
    //    assertEquals(result.getStatus().code(), HttpResponseStatus.NOT_FOUND.code());
    //
    //    assertEquals(result.getPayload().get(), new ApiError("Error: unable to retrieve payload"));
  }
}
