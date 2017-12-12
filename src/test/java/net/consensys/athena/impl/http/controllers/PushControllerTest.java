package net.consensys.athena.impl.http.controllers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageId;
import net.consensys.athena.api.storage.StorageIdBuilder;
import net.consensys.athena.impl.enclave.CesarEnclave;
import net.consensys.athena.impl.http.data.RequestImpl;
import net.consensys.athena.impl.http.data.Result;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.storage.Sha512_256StorageIdBuilder;
import net.consensys.athena.impl.storage.SimpleStorage;
import net.consensys.athena.impl.storage.StorageKeyValueStorageDelegate;
import net.consensys.athena.impl.storage.memory.MemoryStorage;

import java.util.Optional;
import java.util.Random;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Test;

public class PushControllerTest {

  private final Enclave enclave = new CesarEnclave();
  private final StorageIdBuilder keyBuilder = new Sha512_256StorageIdBuilder(enclave);
  private final Storage storage =
      new StorageKeyValueStorageDelegate(new MemoryStorage(), keyBuilder);

  private final Controller controller = new PushController(storage);

  @Test
  public void testPayloadIsStored() throws Exception {
    // generate random byte content
    byte[] toCheck = new byte[342];
    new Random().nextBytes(toCheck);

    // submit request to controller
    EncryptedPayload encryptedPayload = enclave.encrypt(toCheck, null, null);
    Result result = controller.handle(new RequestImpl(Optional.of(encryptedPayload)));

    // ensure we got a 200 OK back
    assertEquals(result.getStatus().code(), HttpResponseStatus.OK.code());

    // ensure result has a payload
    assert (result.getPayload().isPresent());

    // get the id / digest from response, and build our id object
    StorageId id = new SimpleStorage(result.getPayload().get().toString());

    // retrieve stored value
    Optional<StorageData> data = storage.get(id);

    // ensure we fetched something
    assert (data.isPresent());

    // ensure what was stored is what we sent
    assertArrayEquals(
        data.get().getRaw(),
        encryptedPayload
            .getCipherText()); // TODO invalid test, need storage type & refactoring in controller
  }
}
