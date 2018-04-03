package net.consensys.orion.impl.http.handler.push;

import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.storage.Storage;
import net.consensys.orion.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Serializer;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** used to push a payload to a node. */
public class PushHandler implements Handler<RoutingContext> {
  private static final Logger log = LogManager.getLogger();
  private final Storage storage;

  public PushHandler(Storage storage) {
    this.storage = storage;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    EncryptedPayload pushRequest =
        Serializer.deserialize(HttpContentType.CBOR, SodiumEncryptedPayload.class, routingContext.getBody().getBytes());

    // we receive a EncryptedPayload and
    String digest = storage.put(pushRequest);
    log.debug("stored payload. resulting digest: {}", digest);

    // return the digest (key)
    routingContext.response().end(digest);
  }
}
