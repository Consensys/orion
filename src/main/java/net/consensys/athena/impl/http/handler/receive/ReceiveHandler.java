package net.consensys.athena.impl.http.handler.receive;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.enclave.sodium.SodiumPublicKey;
import net.consensys.athena.impl.http.server.HttpContentType;
import net.consensys.athena.impl.utils.Base64;
import net.consensys.athena.impl.utils.Serializer;

import java.util.Optional;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Retrieve a base 64 encoded payload. */
public class ReceiveHandler implements Handler<RoutingContext> {
  private static final Logger log = LogManager.getLogger();
  private final Enclave enclave;
  private final Storage storage;
  private final Serializer serializer;

  public ReceiveHandler(Enclave enclave, Storage storage, Serializer serializer) {
    this.enclave = enclave;
    this.storage = storage;
    this.serializer = serializer;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    log.trace("receive handler called");
    ReceiveRequest receiveRequest =
        serializer.deserialize(
            HttpContentType.JSON, ReceiveRequest.class, routingContext.getBody().getBytes());

    log.debug("got receive request {}", receiveRequest);

    Optional<EncryptedPayload> encryptedPayload = storage.get(receiveRequest.key);
    if (!encryptedPayload.isPresent()) {
      log.info("unable to find payload with key {}", receiveRequest.key);
      routingContext.fail(404);
      return;
    }

    SodiumPublicKey sodiumPublicKey = new SodiumPublicKey(Base64.decode(receiveRequest.to));
    byte[] decryptedPayload = enclave.decrypt(encryptedPayload.get(), sodiumPublicKey);

    // build a ReceiveResponse
    Buffer toReturn =
        Buffer.buffer(
            serializer.serialize(
                HttpContentType.JSON, new ReceiveResponse(Base64.encode(decryptedPayload))));

    routingContext.response().end(toReturn);
  }
}
