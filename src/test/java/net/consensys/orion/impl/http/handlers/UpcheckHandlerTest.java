package net.consensys.orion.impl.http.handlers;

import static org.junit.Assert.assertEquals;

import net.consensys.orion.api.cmd.OrionRoutes;

import okhttp3.Request;
import okhttp3.Response;
import org.junit.Test;

public class UpcheckHandlerTest extends HandlerTest {

  @Test
  public void myApplication() throws Exception {

    Request request = new Request.Builder().get().url(baseUrl + OrionRoutes.UPCHECK).build();

    Response resp = httpClient.newCall(request).execute();
    assertEquals(200, resp.code());
    assertEquals("I'm up!", resp.body().string());
  }
}
