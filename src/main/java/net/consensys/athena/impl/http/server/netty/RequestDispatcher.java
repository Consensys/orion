package net.consensys.athena.impl.http.server.netty;

import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Router;

import java.util.function.BiConsumer;

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
    response = controller.handle(fullHttpRequest, response);

    ctx.writeAndFlush(response);
    fullHttpRequest.release();
  }
}
