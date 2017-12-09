package net.consensys.athena.impl.storage.file;

import static org.junit.Assert.assertEquals;

import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageId;
import net.consensys.athena.impl.storage.SimpleStorage;

import java.util.Optional;

import org.junit.After;
import org.junit.Test;

public class MapDbStorageTest {
  String path = "db";
  MapDbStorage storage = new MapDbStorage(path);

  @After
  public void tearDown() {
    if (storage.isOpen()) {
      storage.close();
    }
  }

  @Test
  public void testStoreAndRetrieve() throws Exception {
    StorageData data = new SimpleStorage("hello".getBytes());
    StorageId key = new SimpleStorage("a key".getBytes());
    storage.put(key, data);
    assertEquals(data, storage.get(key).get());
  }

  @Test
  public void testRetrieveWithoutStore() throws Exception {
    StorageId key = new SimpleStorage("missing".getBytes());
    assertEquals(Optional.empty(), storage.get(key));
  }

  @Test
  public void testStoreAndRetrieveAcrossSessions() throws Exception {
    StorageData data = new SimpleStorage("hello".getBytes());
    StorageId key = new SimpleStorage("a key".getBytes());
    storage.close();
    MapDbStorage secondStorage = new MapDbStorage(path);
    assertEquals(data, secondStorage.get(key).get());
  }
}
