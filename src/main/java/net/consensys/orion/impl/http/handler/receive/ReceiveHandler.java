package net.consensys.orion.impl.http.handler.receive;

import static net.consensys.orion.impl.http.server.HttpContentType.JSON;

import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.enclave.PublicKey;
import net.consensys.orion.api.storage.Storage;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Base64;
import net.consensys.orion.impl.utils.Serializer;

import java.util.Collections;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Retrieve a base 64 encoded payload. */
public class ReceiveHandler implements Handler<RoutingContext> {
  private static final Logger log = LogManager.getLogger();
  private final Enclave enclave;
  private final Storage<EncryptedPayload> storage;
  private final HttpContentType contentType;

  public ReceiveHandler(Enclave enclave, Storage<EncryptedPayload> storage, HttpContentType contentType) {
    this.enclave = enclave;
    this.storage = storage;
    this.contentType = contentType;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    log.trace("receive handler called");
    ReceiveRequest receiveRequest;
    String key;
    PublicKey to = null;
    if (contentType == JSON) {
      receiveRequest = Serializer.deserialize(JSON, ReceiveRequest.class, routingContext.getBody().getBytes());
      log.debug("got receive request {}", receiveRequest);
      key = receiveRequest.key;
      if (receiveRequest.to != null) {
        to = new PublicKey(Base64.decode(receiveRequest.to));
      }
    } else {
      key = routingContext.request().getHeader("c11n-key");
    }
    if (to == null) {
      to = enclave.nodeKeys()[0];
    }
    PublicKey recipient = to;

    storage.get(key).thenAccept(encryptedPayload -> {
      if (!encryptedPayload.isPresent()) {
        log.info("unable to find payload with key {}", key);
        routingContext.fail(404);
        return;
      }

      byte[] decryptedPayload;
      try {
        decryptedPayload = enclave.decrypt(encryptedPayload.get(), recipient);
      } catch (EnclaveException e) {

        log.info("unable to decrypt payload with key {}", key);
        routingContext.fail(404);
        return;
      }

      // configureRoutes a ReceiveResponse
      Buffer toReturn;
      if (contentType == JSON) {
        toReturn = Buffer
            .buffer(Serializer.serialize(JSON, Collections.singletonMap("payload", Base64.encode(decryptedPayload))));
      } else {
        toReturn = Buffer.buffer(decryptedPayload);
      }

      routingContext.response().end(toReturn);
    });
  }
}
