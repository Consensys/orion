package net.consensys.athena.impl.http.controllers;

import static net.consensys.athena.impl.http.data.Result.notFound;
import static net.consensys.athena.impl.http.data.Result.ok;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.http.data.Base64;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Request;
import net.consensys.athena.impl.http.data.Result;
import net.consensys.athena.impl.http.data.Serializer;
import net.consensys.athena.impl.http.server.Controller;

import java.security.PublicKey;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Retrieve a base 64 encoded payload. */
public class ReceiveController implements Controller {
  private final Enclave enclave;
  private final Storage storage;
  private final ContentType contentType;
  private final Serializer serializer;

  public ReceiveController(
      Enclave enclave, Storage storage, ContentType contentType, Serializer serializer) {
    this.enclave = enclave;
    this.storage = storage;
    this.contentType = contentType;
    this.serializer = serializer;
  }

  @Override
  public Result handle(Request request) {
    // retrieves the encrypted payload from DB, using provided key
    ReceiveRequest receiveRequest = request.getPayload();
    Optional<EncryptedPayload> encryptedPayload = storage.get(receiveRequest.key);
    if (!encryptedPayload.isPresent()) {
      return notFound("Error: unable to retrieve payload");
    }

    // Haskell doc: let's check if receipients is set = it's a payload that we sent. TODO @gbotrel
    // if not, it's a payload sent to us
    byte[] decryptedPayload = enclave.decrypt(encryptedPayload.get(), receiveRequest.publicKey);

    // build a ReceiveResponse
    ReceiveResponse toReturn = new ReceiveResponse(Base64.encode(decryptedPayload));
    return ok(contentType, toReturn);
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
