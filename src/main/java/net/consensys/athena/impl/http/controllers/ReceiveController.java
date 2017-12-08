package net.consensys.athena.impl.http.controllers;

import static net.consensys.athena.impl.http.server.Result.internalServerError;
import static net.consensys.athena.impl.http.server.Result.ok;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageKey;
import net.consensys.athena.impl.enclave.EncryptedPayloadBuilder;
import net.consensys.athena.impl.http.server.ContentType;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Result;
import net.consensys.athena.impl.storage.SimpleStorage;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.FullHttpRequest;

/** Retrieve a base 64 encoded payload. */
public class ReceiveController implements Controller {
  private final Enclave enclave;
  private final Storage storage;
  private final ContentType contentType;

  public ReceiveController(Enclave enclave, Storage storage, ContentType contentType) {
    this.enclave = enclave;
    this.storage = storage;
    this.contentType = contentType;
  }

  @Override
  public Result handle(FullHttpRequest request) {
    // TODO @gbotrel: validate request
    ObjectMapper mapper = new ObjectMapper();

    try {
      // retrieves the encrypted payload from DB, using provided key
      ReceiveRequest receiveRequest =
          mapper.readValue(request.content().array(), ReceiveRequest.class);
      StorageKey key = new SimpleStorage(receiveRequest.key);
      Optional<StorageData> data = storage.retrieve(key);
      if (!data.isPresent()) {
        // TODO log error
        return internalServerError(contentType);
      }

      // first, let's build a EncryptedPayload from data
      EncryptedPayload encPayload = new EncryptedPayloadBuilder(data.get().getRaw()).build();
      // Haskell doc: let's check if receipients is set = it's a payload that we sent.
      // if not, it's a payload sent to us
      byte[] decryptedPayload = enclave.decrypt(encPayload, receiveRequest.publicKey);

      // encode in base64 the decryptedPayload
      // build a ReceiveResponse
      ReceiveResponse toReturn =
          new ReceiveResponse(Base64.getEncoder().encodeToString(decryptedPayload));
      return ok(contentType, toReturn);

    } catch (IOException e) {
      e.printStackTrace();
      return internalServerError(contentType);
    }
  }

  static class ReceiveRequest {
    public String key;
    public PublicKey publicKey;

    @JsonCreator
    public ReceiveRequest(
        @JsonProperty("key") String key, @JsonProperty("publicKey") PublicKey publicKey) {
      this.key = key;
      this.publicKey = publicKey;
    }
  }

  static class ReceiveResponse {
    public String payload;

    ReceiveResponse(String payload) {
      this.payload = payload;
    }
  }
}
