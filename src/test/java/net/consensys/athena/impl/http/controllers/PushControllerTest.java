
package net.consensys.athena.impl.http.controllers;

public class PushControllerTest extends ControllerTest {

  //  @Test
  //  public void testPayloadIsStored(TestContext context) {
  //    final Async async = context.async();
  //    // ref to storage
  //    final Storage storage = routes.getStorage();
  //
  //    // build & serialize our payload
  //    SodiumEncryptedPayload encryptedPayload = mockPayload();
  //
  //    // PUSH operation, sending an encrypted payload
  //    Buffer toSend = Buffer.buffer(serializer.serialize(ContentType.CBOR, encryptedPayload));
  //
  //    vertx
  //        .createHttpClient()
  //        .post(
  //            httpServerPort,
  //            "localhost",
  //            AthenaRoutes.PUSH,
  //            response -> {
  //              context.assertEquals(200, response.statusCode());
  //              response.handler(
  //                  body -> {
  //                    // PUSH should return payload digest
  //                    String digest = body.toString();
  //                    context.assertTrue(digest.length() > 0);
  //
  //                    // we should be able to read that from storage
  //                    storage.get(digest);
  //                    Optional<EncryptedPayload> data = storage.get(digest);
  //
  //                    context.assertTrue(data.isPresent());
  //                    context.assertEquals(encryptedPayload, data.get());
  //
  //                    async.complete();
  //                  });
  //            })
  //        .end(toSend);
  //  }
}
