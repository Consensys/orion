package net.consensys.athena.impl.http.server.netty;

import net.consensys.athena.impl.http.server.ContentType;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Result;
import net.consensys.athena.impl.http.server.Router;

import java.nio.charset.Charset;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

  public static final Charset UTF_8 = Charset.forName("UTF-8");
  private Router router;
  private ObjectMapper objectMapper;

  public RequestDispatcher(Router router, ObjectMapper objectMapper) {
    this.router = router;
    this.objectMapper = objectMapper;
  }

  @Override
  public void accept(FullHttpRequest fullHttpRequest, ChannelHandlerContext ctx) {
    ChannelPipeline pipeline = ctx.channel().pipeline();
    if (pipeline.get("chunker") == null) {
      pipeline.addAfter("codec", "chunker", new ChunkedWriteHandler());
    }
    FullHttpResponse response =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    response.headers().add(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);

    Controller controller = router.lookup(fullHttpRequest);
    Result result = controller.handle(fullHttpRequest, response);
    response = outputResponse(fullHttpRequest, result);
    ctx.writeAndFlush(response);
    fullHttpRequest.release();
  }

  private FullHttpResponse outputResponse(FullHttpRequest request, Result result) {
    // TODO lookup content type from the request
    ContentType contentType = result.defaultContentType();
    byte[] bytes = new byte[] {};
    FullHttpResponse response = result.getResponse();
    switch (contentType) {
      case HASKELL_ENCODED:
        // TODO implement or remove
        bytes = new byte[0];
        response.headers().add(HttpHeaderNames.CONTENT_ENCODING, "application/haskell");
        break;
      case JSON:
        try {
          bytes = objectMapper.writeValueAsString(result.getPayload().get()).getBytes(UTF_8);
        } catch (JsonProcessingException e) {
          e.printStackTrace();
          throw new RuntimeException(e);
        }
        response.headers().add(HttpHeaderNames.CONTENT_ENCODING, HttpHeaderValues.APPLICATION_JSON);
        break;
      case RAW:
        //TODO revisit
        bytes = result.getPayload().get().toString().getBytes(UTF_8);
        response
            .headers()
            .add(HttpHeaderNames.CONTENT_ENCODING, HttpHeaderValues.APPLICATION_OCTET_STREAM);
        break;
    }
    response = response.setStatus(result.getStatus());
    ByteBuf content = Unpooled.copiedBuffer(bytes);

    return response.replace(content);
  }
}
