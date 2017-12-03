package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.http.responders.SendResponder;
import net.consensys.athena.impl.http.server.ContentType;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Responder;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

/** Send a base64 encoded payload to encrypt. */
public class SendController implements Controller {
  private final Enclave enclave;
  private final Storage storage;

  public SendController(Enclave enclave, Storage storage, ContentType json) {
    this.enclave = enclave;
    this.storage = storage;
  }

  @Override
  public Responder handle(HttpRequest request, FullHttpResponse response) {
    return new SendResponder(response, ContentType.JSON);
  }
}
