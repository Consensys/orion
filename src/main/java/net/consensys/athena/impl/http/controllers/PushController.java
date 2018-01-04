package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Serializer;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** used to push a payload to a node. */
public class PushController implements Handler<RoutingContext> {
  private static final Logger log = LogManager.getLogger();
  private final Storage storage;
  private final Serializer serializer;

  public PushController(Storage storage, Serializer serializer) {
    this.serializer = serializer;
    this.storage = storage;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    EncryptedPayload pushRequest =
        serializer.deserialize(
            ContentType.CBOR, EncryptedPayload.class, routingContext.getBody().getBytes());

    // we receive a EncryptedPayload and
    String digest = storage.put(pushRequest);
    log.debug("stored payload. resulting digest: {}", digest);

    // return the digest (key)
    routingContext.response().end(digest);
  }
}
