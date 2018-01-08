package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.cmd.AthenaRoutes;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Test;

public class UpcheckControllerTest extends ControllerTest {
  public static final UpcheckController controller = new UpcheckController();

  @Test
  public void testMyApplication(TestContext context) {
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
  }
}
