package net.consensys.orion.impl.http.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

class UpcheckHandlerTest extends HandlerTest {

  @Test
  void publicUpcheck() throws Exception {
    Request request = new Request.Builder().get().url(nodeBaseUrl + "/upcheck").build();

    Response resp = httpClient.newCall(request).execute();
    assertEquals(200, resp.code());
    assertEquals("I'm up!", resp.body().string());
  }

  @Test
  void privateUpcheck() throws Exception {
    Request request = new Request.Builder().get().url(clientBaseUrl + "/upcheck").build();

    Response resp = httpClient.newCall(request).execute();
    assertEquals(200, resp.code());
    assertEquals("I'm up!", resp.body().string());
  }
}
