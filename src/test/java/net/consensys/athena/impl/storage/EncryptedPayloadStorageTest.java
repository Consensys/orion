package net.consensys.athena.impl.storage;

import static junit.framework.TestCase.assertEquals;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageKeyBuilder;
import net.consensys.athena.impl.enclave.LibSodiumEnclaveStub;
import net.consensys.athena.impl.storage.memory.MemoryStorage;

import java.util.Optional;
import java.util.Random;

import org.junit.Test;

public class EncryptedPayloadStorageTest {

  private Enclave enclave = new LibSodiumEnclaveStub();
  private StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder(enclave);
  MemoryStorage memory = new MemoryStorage();
  Storage<EncryptedPayload> storage = new EncryptedPayloadStorage(memory, keyBuilder);

  @Test
  public void testStoreAndRetrieve() throws Exception {
    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    EncryptedPayload toStore = enclave.encrypt(toEncrypt, null, null);

    String key = storage.put(toStore);
    assertEquals(toStore, storage.get(key).get());
  }

  @Test
  public void testRetrieveWithoutStore() throws Exception {
    assertEquals(Optional.empty(), storage.get("missing"));
  }
}
