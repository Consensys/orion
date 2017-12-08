package net.consensys.athena.impl.http.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Optional;
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

/** Retrieve a base 64 encoded payload. */
public class ReceiveController implements Controller {
  private final Enclave enclave;
  private final Storage storage;
  private ContentType contentType;

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
        return Result.internalServerError(ContentType.JSON);
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
      // convert it to JSON
      // write it to the response

    } catch (IOException e) {
      return Result.internalServerError(ContentType.JSON);
    }

    return Result.ok(ContentType.JSON, null);

  }

  private static class ReceiveRequest {
    public String key;
    public PublicKey publicKey;
  }

  private static class ReceiveResponse {
    public String payload;

    ReceiveResponse(String payload) {
      this.payload = payload;
    }
  }
}
