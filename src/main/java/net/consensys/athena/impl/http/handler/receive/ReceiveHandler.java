package net.consensys.athena.impl.http.handler.receive;

import static net.consensys.athena.impl.http.server.HttpContentType.JSON;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.enclave.sodium.SodiumPublicKey;
import net.consensys.athena.impl.http.server.HttpContentType;
import net.consensys.athena.impl.utils.Base64;
import net.consensys.athena.impl.utils.Serializer;

import java.security.PublicKey;
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
  private final HttpContentType contentType;

  public ReceiveHandler(
      Enclave enclave, Storage storage, Serializer serializer, HttpContentType contentType) {
    this.enclave = enclave;
    this.storage = storage;
    this.serializer = serializer;
    this.contentType = contentType;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    log.trace("receive handler called");
    ReceiveRequest receiveRequest;
    String key;
    PublicKey to = null;
    if (contentType == JSON) {
      receiveRequest =
          serializer.deserialize(JSON, ReceiveRequest.class, routingContext.getBody().getBytes());
      log.debug("got receive request {}", receiveRequest);
      key = receiveRequest.key;
      if (receiveRequest.to != null) {
        to = new SodiumPublicKey(Base64.decode(receiveRequest.to));
      }
    } else {
      key = routingContext.request().getHeader("c11n-key");
    }
    if (to == null) {
      to = enclave.nodeKeys()[0];
    }

    Optional<EncryptedPayload> encryptedPayload = storage.get(key);
    if (!encryptedPayload.isPresent()) {
      log.info("unable to find payload with key {}", key);
      routingContext.fail(404);
      return;
    }

    byte[] decryptedPayload = enclave.decrypt(encryptedPayload.get(), to);

    // build a ReceiveResponse
    Buffer toReturn;
    if (contentType == JSON) {
      toReturn =
          Buffer.buffer(
              serializer.serialize(JSON, new ReceiveResponse(Base64.encode(decryptedPayload))));
    } else {
      toReturn = Buffer.buffer(decryptedPayload);
    }

    routingContext.response().end(toReturn);
  }
}
