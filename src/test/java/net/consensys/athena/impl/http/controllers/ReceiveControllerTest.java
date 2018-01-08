package net.consensys.athena.impl.http.controllers;

public class ReceiveControllerTest extends ControllerTest {
  //
  //  @Test
  //  public void testPayloadIsRetrieved() {
  //    // ref to storage
  //    final Storage storage = routes.getStorage();
  //
  //    // build & serialize our payload
  //    SodiumEncryptedPayload encryptedPayload = mockPayload();
  //
  //    // store it
  //    String key = storage.put(encryptedPayload);
  //
  //    // and try to retrieve it with the receiveController
  //    ReceiveRequest req = new ReceiveRequest(key, null);
  //
  //    // RECEIVE  operation, sending a ReceiveRequest
  //    Buffer toSend = Buffer.buffer(serializer.serialize(ContentType.JSON, req));
  //
  //    vertx
  //        .createHttpClient()
  //        .post(
  //            httpServerPort,
  //            "localhost",
  //            AthenaRoutes.RECIEVE,
  //            response -> {
  //              context.assertEquals(200, response.statusCode());
  //              async.complete();
  //              response.handler(
  //                  body -> {
  //                    // TODO check ReceiveResponse
  //                    //    // read response
  //                    //    ReceiveResponse response = (ReceiveResponse) result.getPayload().get();
  //                    //
  //                    //    // ensure we got the decrypted response back
  //                    //    assertArrayEquals(Base64.decode(response.payload), toEncrypt);
  //                  });
  //            })
  //        .end(toSend);
  //  }
  //
  //  @Test
  //  public void testResponseWhenKeyNotFound() throws Exception {
  //    // and try to retrieve it with the receiveController
  //    ReceiveRequest req = new ReceiveRequest("notForMe", null);
  //
  //    // RECEIVE  operation, sending a ReceiveRequest
  //    Buffer toSend = Buffer.buffer(serializer.serialize(ContentType.JSON, req));
  //
  //    vertx
  //        .createHttpClient()
  //        .post(
  //            httpServerPort,
  //            "localhost",
  //            AthenaRoutes.RECIEVE,
  //            response -> {
  //              context.assertEquals(404, response.statusCode());
  //              async.complete();
  //            })
  //        .end(toSend);
  //  }
}
