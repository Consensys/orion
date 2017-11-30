package net.consensys.athena.impl.http.helpers;

import net.consensys.athena.impl.http.server.Controller;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/** Helper class to test Http endpoints (Netty controllers ) * */
public class HttpTester {
  private final Controller controller;
  private String uri = "/";
  private HttpMethod method = HttpMethod.GET;
  private byte[] payload = null;

  public HttpTester(Controller controller) {
    this.controller = controller;
  }

  public FullHttpResponse sendRequest() {
    // create Netty payload buffer
    ByteBuf buffPayload;
    if (payload != null && payload.length > 0) {
      buffPayload = Unpooled.copiedBuffer(payload);
    } else {
      buffPayload = Unpooled.buffer(0);
    }

    // building request
    FullHttpRequest req =
        new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri, buffPayload);

    // call the controller with fake request / response
    FullHttpResponse defaultResponse =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

    return controller.handle(req, defaultResponse);
  }

  public HttpTester uri(String uri) {
    this.uri = uri;
    return this;
  }

  public HttpTester method(HttpMethod method) {
    this.method = method;
    return this;
  }

  public HttpTester payload(byte[] payload) {
    this.payload = payload;
    return this;
  }
}
