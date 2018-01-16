package net.consensys.athena.impl.http.handler.push;

import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.athena.impl.http.server.HttpContentType;
import net.consensys.athena.impl.utils.Serializer;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** used to push a payload to a node. */
public class PushHandler implements Handler<RoutingContext> {
  private static final Logger log = LogManager.getLogger();
  private final Storage storage;
  private final Serializer serializer;

  public PushHandler(Storage storage, Serializer serializer) {
    this.serializer = serializer;
    this.storage = storage;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    SodiumEncryptedPayload pushRequest =
        serializer.deserialize(
            HttpContentType.CBOR,
            SodiumEncryptedPayload.class,
            routingContext.getBody().getBytes());

    // we receive a EncryptedPayload and
    String digest = storage.put(pushRequest);
    log.debug("stored payload. resulting digest: {}", digest);

    // return the digest (key)
    routingContext.response().end(digest);
  }
}
