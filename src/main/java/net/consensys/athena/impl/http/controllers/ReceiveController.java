package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageKey;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.storage.SimpleStorage;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

/** Retrieve a base 64 encoded payload. */
public class ReceiveController implements Controller {
  private final Enclave enclave;
  private final Storage storage;

  public ReceiveController(Enclave enclave, Storage storage) {
    this.enclave = enclave;
    this.storage = storage;
  }

  @Override
  public FullHttpResponse handle(FullHttpRequest request, FullHttpResponse response) {
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
        return response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
      }

      // first, let's build a EncryptedPayload from data
      //      EncryptedPayload encPayload = new EncryptedPayloadBuilder(data.get().getRaw()).build();
      // let's check if receipients is set = it's a payload that we sent.
      // if not, it's a payload sent to us
      // call enclave.decrypt(encPayload, receiveRequest.key)

      // encode in base64 the decryptedPayload
      // build a ReceiveResponse
      // convert it to JSON
      // write it to the response

    } catch (IOException e) {
      return response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    return response;
  }

  private static class ReceiveRequest {
    public String key;
    public PublicKey publicKey;
  }

  private static class ReceiveResponse {
    public String payload;
  }
}
