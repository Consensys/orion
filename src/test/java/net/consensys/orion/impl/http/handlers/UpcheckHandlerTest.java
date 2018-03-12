package net.consensys.orion.impl.http.handlers;

import static org.junit.Assert.assertEquals;

import net.consensys.orion.api.cmd.OrionRoutes;

import okhttp3.Request;
import okhttp3.Response;
import org.junit.Test;

public class UpcheckHandlerTest extends HandlerTest {

  @Test
  public void testPublicUpcheck() throws Exception {

    Request request = new Request.Builder().get().url(publicBaseUrl + OrionRoutes.UPCHECK).build();

    Response resp = httpClient.newCall(request).execute();
    assertEquals(200, resp.code());
    assertEquals("I'm up!", resp.body().string());
  }

  @Test
  public void testPrivateUpcheck() throws Exception {

    Request request = new Request.Builder().get().url(privateBaseUrl + OrionRoutes.UPCHECK).build();

    Response resp = httpClient.newCall(request).execute();
    assertEquals(200, resp.code());
    assertEquals("I'm up!", resp.body().string());
  }
}
