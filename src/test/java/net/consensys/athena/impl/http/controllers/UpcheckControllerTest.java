package net.consensys.athena.impl.http.controllers;

import static org.junit.Assert.*;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.Test;

public class UpcheckControllerTest {
  public static final UpcheckController controller = new UpcheckController();

  @Test
  public void TestHandleStringReturnsAString() throws Exception {
    assertEquals("I'm up!\n", controller.stringResponse());
  }

  @Test
  public void testResponseIsImUp() {
    FullHttpResponse response =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    FullHttpResponse res = controller.handle(null, response);
    String s = new String(res.content().array());
    assertEquals("I'm up!\n", s);
  }
}
