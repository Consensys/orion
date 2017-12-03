package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.http.responders.ReceiveResponder;
import net.consensys.athena.impl.http.server.ContentType;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Responder;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

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
  public Responder handle(HttpRequest request, FullHttpResponse response) {
    return new ReceiveResponder(response, contentType);
  }
}
