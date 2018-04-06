package net.consensys.orion.impl.http.handlers;

import static org.junit.Assert.assertEquals;

import okhttp3.Request;
import okhttp3.Response;
import org.junit.Test;

public class UpcheckHandlerTest extends HandlerTest {

  @Test
  public void publicUpcheck() throws Exception {

    Request request = new Request.Builder().get().url(nodeBaseUrl + "/upcheck").build();

    Response resp = httpClient.newCall(request).execute();
    assertEquals(200, resp.code());
    assertEquals("I'm up!", resp.body().string());
  }

  @Test
  public void privateUpcheck() throws Exception {

    Request request = new Request.Builder().get().url(clientBaseUrl + "/upcheck").build();

    Response resp = httpClient.newCall(request).execute();
    assertEquals(200, resp.code());
    assertEquals("I'm up!", resp.body().string());
  }
}
