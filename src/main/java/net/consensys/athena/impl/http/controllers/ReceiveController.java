package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.enclave.sodium.SodiumPublicKey;
import net.consensys.athena.impl.http.data.Base64;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Serializer;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

/** Retrieve a base 64 encoded payload. */
public class ReceiveController implements Handler<RoutingContext> {
  private final Enclave enclave;
  private final Storage storage;
  private final Serializer serializer;

  public ReceiveController(Enclave enclave, Storage storage, Serializer serializer) {
    this.enclave = enclave;
    this.storage = storage;
    this.serializer = serializer;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    ReceiveRequest receiveRequest =
        serializer.deserialize(
            ContentType.JSON, ReceiveRequest.class, routingContext.getBody().getBytes());

    Optional<EncryptedPayload> encryptedPayload = storage.get(receiveRequest.key);
    if (!encryptedPayload.isPresent()) {
      routingContext.fail(404);
      return;
    }

    // Haskell doc: let's check if receipients is set = it's a payload that we sent. TODO @gbotrel
    // if not, it's a payload sent to us
    byte[] decryptedPayload = enclave.decrypt(encryptedPayload.get(), receiveRequest.publicKey);

    // build a ReceiveResponse
    Buffer toReturn =
        Buffer.buffer(
            serializer.serialize(
                ContentType.JSON, new ReceiveResponse(Base64.encode(decryptedPayload))));

    routingContext.response().end(toReturn);
  }

  static class ReceiveRequest {
    public String key;
    public SodiumPublicKey publicKey;

    @JsonCreator
    public ReceiveRequest(
        @JsonProperty("key") String key, @JsonProperty("publicKey") SodiumPublicKey publicKey) {
      this.key = key;
      this.publicKey = publicKey;
    }
  }

  static class ReceiveResponse {
    public String payload;

    @JsonCreator
    ReceiveResponse(@JsonProperty("payload") String payload) {
      this.payload = payload;
    }
  }
}
