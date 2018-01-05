package net.consensys.athena.impl.http.server.vertx;

import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Serializer;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class RequestSerializationHandler implements Handler<RoutingContext> {

  private final Serializer serializer;
  private final ContentType contentType;
  private final Class<?> valueType;

  public RequestSerializationHandler(
      Serializer serializer, ContentType contentType, Class<?> valueType) {
    this.serializer = serializer;
    this.contentType = contentType;
    this.valueType = valueType;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    Object payload =
        serializer.deserialize(contentType, valueType, routingContext.getBody().getBytes());
    routingContext.put("request", payload);
    routingContext.next();
  }
}
