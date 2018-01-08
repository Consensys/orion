package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.cmd.AthenaRoutes;
import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.impl.http.data.ContentType;

import java.net.URL;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Test;

public class PartyInfoControllerTest extends ControllerTest {

  @Test(timeout = 200L)
  public void testSuccessfulProcessingOfRequest(TestContext context) throws Exception {
    networkNodes.addNodeURL(new URL("http://127.0.0.1:9001/"));
    networkNodes.addNodeURL(new URL("http://127.0.0.1:9002/"));

    final Async async = context.async();
    vertx
        .createHttpClient()
        .getNow(
            httpServerPort,
            "localhost",
            AthenaRoutes.PARTYINFO,
            response -> {
              context.assertEquals(200, response.statusCode());
              response.handler(
                  body -> {
                    NetworkNodes partyInfoResponse =
                        serializer.deserialize(
                            ContentType.CBOR, NetworkNodes.class, body.getBytes());
                    context.assertEquals(networkNodes, partyInfoResponse);
                    async.complete();
                  });
            });
  }
}
