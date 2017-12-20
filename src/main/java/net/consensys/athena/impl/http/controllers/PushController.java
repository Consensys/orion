package net.consensys.athena.impl.http.controllers;

import static net.consensys.athena.impl.http.data.Result.ok;

import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Request;
import net.consensys.athena.impl.http.data.Result;
import net.consensys.athena.impl.http.server.Controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** used to push a payload to a node. */
public class PushController implements Controller {
  private static final Logger log = LogManager.getLogger();
  private final Storage storage;

  public PushController(Storage storage) {
    this.storage = storage;
  }

  @Override
  public Class<?> expectedRequest() {
    return EncryptedPayload.class;
  }

  @Override
  public Result handle(Request request) {
    // that's actually useful to ensure we don't get random bytes as input
    EncryptedPayload pushRequest = request.getPayload();

    // we receive a EncryptedPayload and
    String digest = storage.put(pushRequest);

    log.debug("stored payload. resulting digest: {}", digest);

    // return the digest (key)
    return ok(ContentType.JSON, digest);
  }
}
