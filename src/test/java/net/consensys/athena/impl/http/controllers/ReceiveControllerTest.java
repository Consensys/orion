package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageKeyBuilder;
import net.consensys.athena.impl.enclave.BouncyCastleEnclave;
import net.consensys.athena.impl.http.server.ContentType;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.storage.Sha512_256StorageKeyBuilder;
import net.consensys.athena.impl.storage.memory.MemoryStorage;

import org.junit.Test;

public class ReceiveControllerTest {
  private final Enclave enclave = new BouncyCastleEnclave();
  private final StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder(enclave);
  private final Storage storage = new MemoryStorage(keyBuilder);

  private final Controller receiveController =
      new ReceiveController(enclave, storage, ContentType.JSON);

  @Test
  public void testPayloadIsRetrieved() throws Exception {
    // TODO
  }
}
