package net.consensys.athena.impl.storage;

import static org.junit.Assert.assertEquals;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageId;
import net.consensys.athena.api.storage.StorageIdBuilder;
import net.consensys.athena.impl.enclave.BouncyCastleEnclave;
import net.consensys.athena.impl.storage.memory.MemoryStorage;

import java.util.Optional;

import org.junit.Test;

public class StorageKeyValueStorageDelegateTest {
  private Enclave enclave = new BouncyCastleEnclave();
  private StorageIdBuilder keyBuilder = new Sha512_256StorageIdBuilder(enclave);
  MemoryStorage memory = new MemoryStorage();
  StorageKeyValueStorageDelegate storage = new StorageKeyValueStorageDelegate(memory, keyBuilder);

  @Test
  public void testStoreAndRetrieve() throws Exception {
    StorageData data = new SimpleStorage("hello".getBytes());
    StorageId key = storage.put(data);
    assertEquals(data, storage.get(key).get());
  }

  @Test
  public void testRetrieveWithoutStore() throws Exception {
    StorageId key = new SimpleStorage("missing".getBytes());
    assertEquals(Optional.empty(), storage.get(key));
  }
}
