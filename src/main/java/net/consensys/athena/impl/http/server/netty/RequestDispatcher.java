package net.consensys.athena.impl.http.server.netty;

import net.consensys.athena.impl.http.server.ContentType;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Responder;
import net.consensys.athena.impl.http.server.Router;

import java.nio.charset.Charset;
import java.util.function.BiConsumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.stream.ChunkedWriteHandler;

public class RequestDispatcher implements BiConsumer<FullHttpRequest, ChannelHandlerContext> {
  Router router;

  public RequestDispatcher(Router router) {
    this.router = router;
  }

  @Override
  public void accept(FullHttpRequest fullHttpRequest, ChannelHandlerContext ctx) {
    ChannelPipeline pipeline = ctx.channel().pipeline();
    if (pipeline.get("chunker") == null) {
      pipeline.addAfter("codec", "chunker", new ChunkedWriteHandler());
    }
    FullHttpResponse response =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    response.headers().add(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);

    Controller controller = router.lookup(fullHttpRequest);
    Responder responder = controller.handle(fullHttpRequest, response);
    response = outputResponse(fullHttpRequest, responder);
    ctx.writeAndFlush(response);
    fullHttpRequest.release();
  }

  private FullHttpResponse outputResponse(FullHttpRequest request, Responder responder) {
    //TODO lookup content type from the request
    ContentType contentType = responder.defaultContentType();
    byte[] bytes = new byte[] {};
    switch (contentType) {
      case HASKELL_ENCODED:
        bytes = responder.getHaskellEncoded();
        break;
      case JSON:
        bytes = responder.getJson().getBytes(Charset.defaultCharset());
        break;
      case RAW:
        bytes = responder.getRaw();
        break;
    }
    ByteBuf content = Unpooled.copiedBuffer(bytes);
    return responder.getResponse().replace(content);
  }
}
