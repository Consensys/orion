package net.consensys.athena.impl.http.controllers;

import static net.consensys.athena.impl.http.server.Result.ok;

import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageId;
import net.consensys.athena.impl.http.server.ContentType;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Request;
import net.consensys.athena.impl.http.server.Result;
import net.consensys.athena.impl.storage.SimpleStorage;

import java.util.Optional;

/** used to push a payload to a node. */
public class PushController implements Controller {
  private final Storage storage;

  public PushController(Storage storage) {
    this.storage = storage;
  }

  @Override
  public Optional<Class<?>> expectedRequest() {
    return Optional.of(EncryptedPayload.class);
  }

  @Override
  public Result handle(Request request) {
    EncryptedPayload pushRequest = request.getPayload();

    // we receive a EncryptedPayload and TODO the storage should be typed with that
    StorageData toStore =
        new SimpleStorage(
            pushRequest.getCipherText()); // need typed storage, getCipherText to be removed.
    StorageId digest = storage.put(toStore);

    // return the digest (key)
    return ok(ContentType.JSON, digest.getBase64Encoded());
  }
}
