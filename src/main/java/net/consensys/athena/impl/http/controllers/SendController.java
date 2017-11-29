package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.storage.Storage;

/** Send a base64 encoded payload to encrypt. */
public class SendController extends StringResponseController {
  private final Enclave enclave;
  private final Storage storage;

  public SendController(Enclave enclave, Storage storage) {
    this.enclave = enclave;
    this.storage = storage;
  }

  @Override
  protected String stringResponse() {
    return "{\"key\":\"abcd\"}";
  }
}
