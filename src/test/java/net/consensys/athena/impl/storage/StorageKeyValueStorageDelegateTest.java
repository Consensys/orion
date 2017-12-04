package net.consensys.athena.impl.storage;

import static org.junit.Assert.assertEquals;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageKey;
import net.consensys.athena.api.storage.StorageKeyBuilder;
import net.consensys.athena.impl.enclave.BouncyCastleEnclave;
import net.consensys.athena.impl.storage.memory.MemoryStorage;

import java.util.Optional;

import org.junit.Test;

public class StorageKeyValueStorageDelegateTest {
  private Enclave enclave = new BouncyCastleEnclave();
  private StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder(enclave);
  MemoryStorage memory = new MemoryStorage();
  StorageKeyValueStorageDelegate storage = new StorageKeyValueStorageDelegate(memory, keyBuilder);

  @Test
  public void testStoreAndRetrieve() throws Exception {
    StorageData data = new SimpleStorage("hello".getBytes());
    StorageKey key = storage.store(data);
    assertEquals(data, storage.retrieve(key).get());
  }

  @Test
  public void testRetrieveWithoutStore() throws Exception {
    StorageKey key = new SimpleStorage("missing".getBytes());
    assertEquals(Optional.empty(), storage.retrieve(key));
  }
}
