package net.consensys.athena.impl.storage.memory;

import static org.junit.Assert.*;

import net.consensys.athena.api.enclave.BouncyCastleEnclave;
import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageKey;
import net.consensys.athena.api.storage.StorageKeyBuilder;
import net.consensys.athena.impl.storage.Sha512_256StorageKeyBuilder;
import net.consensys.athena.impl.storage.SimpleStorage;

import org.junit.Test;

public class MemoryStorageTest {
  private Enclave enclave = new BouncyCastleEnclave();
  private StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder(enclave);
  MemoryStorage storage = new MemoryStorage(keyBuilder);

  @Test
  public void testStoreAndRetrieve() throws Exception {
    StorageData data = new SimpleStorage("hello".getBytes());
    StorageKey key = storage.store(data);
    assertEquals(data, storage.retrieve(key));
  }

  @Test
  public void testRetrieveWithoutStore() throws Exception {
    StorageKey key = new SimpleStorage("missing".getBytes());
    assertNull(storage.retrieve(key));
  }
}
