package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.impl.http.server.Controller;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

public abstract class HandleStringController implements Controller {

  public FullHttpResponse handle(HttpRequest request, FullHttpResponse response) {
    ByteBuf content = Unpooled.copiedBuffer(handleString().getBytes(Charset.forName("utf8")));
    return response.replace(content);
  }

  protected abstract String handleString();
}
