package net.consensys.orion.impl.http.handler.push;

import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.storage.Storage;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Serializer;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** used to push a payload to a node. */
public class PushHandler implements Handler<RoutingContext> {
  private static final Logger log = LogManager.getLogger();
  private final Storage<EncryptedPayload> storage;

  public PushHandler(Storage<EncryptedPayload> storage) {
    this.storage = storage;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    EncryptedPayload pushRequest =
        Serializer.deserialize(HttpContentType.CBOR, EncryptedPayload.class, routingContext.getBody().getBytes());

    storage.put(pushRequest).thenAccept((digest) -> {
      log.debug("stored payload. resulting digest: {}", digest);
      routingContext.response().end(digest);
    }).exceptionally(e -> routingContext.fail(e));

  }
}
