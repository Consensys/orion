package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.impl.http.server.Controller;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

public abstract class StringResponseController implements Controller {

  public FullHttpResponse handle(FullHttpRequest request, FullHttpResponse response) {
    ByteBuf content = Unpooled.copiedBuffer(stringResponse().getBytes(Charset.forName("utf8")));
    return response.replace(content);
  }

  protected abstract String stringResponse();
}
