package net.consensys.athena.impl.http.server.vertx;

import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Serializer;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

public class ResponseSerializationHandler implements Handler<RoutingContext> {

  private final Serializer serializer;
  private final ContentType contentType;

  public ResponseSerializationHandler(Serializer serializer, ContentType contentType) {
    this.serializer = serializer;
    this.contentType = contentType;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    Buffer toReturn =
        Buffer.buffer(serializer.serialize(contentType, routingContext.get("response")));
    routingContext.response().end(toReturn);
  }
}
