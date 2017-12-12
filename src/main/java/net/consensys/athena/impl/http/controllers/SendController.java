package net.consensys.athena.impl.http.controllers;

import static net.consensys.athena.impl.http.server.Result.notImplemented;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.http.server.ContentType;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Request;
import net.consensys.athena.impl.http.server.Result;

/** Send a base64 encoded payload to encrypt. */
public class SendController implements Controller {
  private final Enclave enclave;
  private final Storage storage;
  private ContentType contentType;

  public SendController(Enclave enclave, Storage storage, ContentType contentType) {
    this.enclave = enclave;
    this.storage = storage;
    this.contentType = contentType;
  }

  @Override
  public Result handle(Request request) {
    return notImplemented();
  }
}
