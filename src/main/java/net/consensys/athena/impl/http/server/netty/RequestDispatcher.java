package net.consensys.athena.impl.http.server.netty;

import static net.consensys.athena.impl.http.server.Result.internalServerError;

import net.consensys.athena.impl.http.server.ContentType;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Result;
import net.consensys.athena.impl.http.server.Router;
import net.consensys.athena.impl.http.server.Serializer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;
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
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RequestDispatcher implements BiConsumer<FullHttpRequest, ChannelHandlerContext> {
  private static final Logger log = LogManager.getLogger();
  public static final Charset UTF_8 = Charset.forName("UTF-8");
  private Router router;
  private Serializer serializer;

  public RequestDispatcher(Router router, Serializer serializer) {
    this.router = router;
    this.serializer = serializer;
  }

  @Override
  public void accept(FullHttpRequest fullHttpRequest, ChannelHandlerContext ctx) {
    ChannelPipeline pipeline = ctx.channel().pipeline();
    if (pipeline.get("chunker") == null) {
      pipeline.addAfter("codec", "chunker", new ChunkedWriteHandler());
    }

    Controller controller = router.lookup(fullHttpRequest);
    // log incoming http request
    log.debug(
        "processing {} request on {} with {}",
        fullHttpRequest.method().name(),
        fullHttpRequest.uri(),
        controller.getClass().getSimpleName());

    // deserialize request payload

    Result result;
    try {
      // process http request
      result = controller.handle(fullHttpRequest);
    } catch (Exception e) {
      // if an exception occurred, return a formatted ApiError
      log.error(e.getMessage());
      result = internalServerError(e.getMessage());
    }

    // build httpResponse
    FullHttpResponse response = outputResponse(fullHttpRequest, result);
    ctx.writeAndFlush(response);
    fullHttpRequest.release();
  }

  private FullHttpResponse outputResponse(FullHttpRequest request, Result result) {
    // TODO lookup content type from the request
    FullHttpResponse response =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    response.headers().add(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
    // TODO check result.getPayload().isPresent();
    ContentType contentType = result.defaultContentType();
    byte[] bytes;
    switch (contentType) {
      case JSON:
        try {
          bytes = serializer.serialize(result.getPayload().get(), contentType);
        } catch (IOException e) {
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
      default:
        throw new RuntimeException("specified contentType is not implemented");
    }
    Optional<HttpHeaders> extraHeaders = result.getExtraHeaders();
    if (extraHeaders.isPresent()) {
      HttpHeaders headers = extraHeaders.get();
      response.headers().add(headers);
    }
    response = response.setStatus(result.getStatus());
    ByteBuf content = Unpooled.copiedBuffer(bytes);

    return response.replace(content);
  }
}
