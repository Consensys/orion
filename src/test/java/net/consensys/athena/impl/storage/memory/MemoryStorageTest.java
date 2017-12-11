package net.consensys.athena.impl.storage.memory;

import static org.junit.Assert.assertEquals;

import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageId;
import net.consensys.athena.impl.storage.SimpleStorage;

import java.util.Optional;

import org.junit.Test;

public class MemoryStorageTest {
  MemoryStorage storage = new MemoryStorage();

  @Test
  public void testStoreAndRetrieve() throws Exception {
    StorageData data = new SimpleStorage("hello".getBytes());
    StorageId key = new SimpleStorage("key".getBytes());
    storage.put(key, data);
    assertEquals(data, storage.get(key).get());
  }

  @Test
  public void testRetrieveWithoutStore() throws Exception {
    StorageId key = new SimpleStorage("missing".getBytes());
    assertEquals(Optional.empty(), storage.get(key));
  }
}
