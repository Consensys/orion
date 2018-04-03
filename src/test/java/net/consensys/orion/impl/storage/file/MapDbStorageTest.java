package net.consensys.orion.impl.storage.file;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.junit.After;
import org.junit.Test;

public class MapDbStorageTest {
  String path = ".";
  MapDbStorage<String> storage = new MapDbStorage<>(String.class, path);

  private final String toStore = "data";

  public MapDbStorageTest() {}

  @After
  public void tearDown() {
    if (storage.isOpen()) {
      storage.close();
    }
  }

  @Test
  public void storeAndRetrieve() {
    storage.put("key", toStore);
    assertEquals(toStore, storage.get("key").get());
  }

  @Test
  public void retrieveWithoutStore() {
    assertEquals(Optional.empty(), storage.get("missing"));
  }

  @Test
  public void storeAndRetrieveAcrossSessions() {
    storage.put("key", toStore);
    storage.close();
    MapDbStorage<byte[]> secondStorage = new MapDbStorage(String.class, path);
    assertEquals(toStore, secondStorage.get("key").get());
  }

  @Test
  public void storeAndRemove() {
    storage.put("key", toStore);
    storage.remove("key");
    assertEquals(Optional.empty(), storage.get("key"));
  }
}
