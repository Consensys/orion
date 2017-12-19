package net.consensys.athena.impl.http.controllers;

import static net.consensys.athena.impl.http.data.Result.internalServerError;
import static net.consensys.athena.impl.http.data.Result.ok;

import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageId;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Request;
import net.consensys.athena.impl.http.data.Result;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Serializer;
import net.consensys.athena.impl.storage.SimpleStorage;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** used to push a payload to a node. */
public class PushController implements Controller {
  private static final Logger log = LogManager.getLogger();
  private final Storage storage;
  private final Serializer serializer;

  public PushController(Storage storage, Serializer serializer) {
    this.storage = storage;
    this.serializer = serializer;
  }

  @Override
  public Class<?> expectedRequest() {
    return EncryptedPayload.class;
  }

  @Override
  public Result handle(Request request) {
    // that's actually useful to ensure we don't get random bytes as input
    EncryptedPayload pushRequest = request.getPayload();

    // serialize encrypted payload for storage
    byte[] bPayload = new byte[0];
    try {
      bPayload = serializer.serialize(pushRequest, ContentType.CBOR);
    } catch (IOException e) {
      log.error(e.getMessage());
      return internalServerError(e.getMessage());
    }

    // we receive a EncryptedPayload and TODO the storage should be typed with that
    StorageData toStore = new SimpleStorage(bPayload);
    StorageId digest = storage.put(toStore);

    // return the digest (key)
    return ok(ContentType.JSON, digest.getBase64Encoded());
  }
}
