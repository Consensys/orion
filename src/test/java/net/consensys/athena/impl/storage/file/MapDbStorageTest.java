package net.consensys.athena.impl.storage.file;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.util.Optional;

import org.junit.After;
import org.junit.Test;

public class MapDbStorageTest {
  String path = "db";
  MapDbStorage<byte[]> storage = new MapDbStorage<>(path);

  final byte[] toStore = "data".getBytes("UTF-8");

  public MapDbStorageTest() throws UnsupportedEncodingException {}

  @After
  public void tearDown() {
    if (storage.isOpen()) {
      storage.close();
    }
  }

  @Test
  public void testStoreAndRetrieve() {
    storage.put("key", toStore);
    assertArrayEquals(toStore, storage.get("key").get());
  }

  @Test
  public void testRetrieveWithoutStore() {
    assertEquals(Optional.empty(), storage.get("missing"));
  }

  @Test
  public void testStoreAndRetrieveAcrossSessions() {
    storage.put("key", toStore);
    storage.close();
    MapDbStorage<byte[]> secondStorage = new MapDbStorage(path);
    assertArrayEquals(toStore, secondStorage.get("key").get());
  }

  @Test
  public void testStoreAndRemove() {
    storage.put("key", toStore);
    storage.remove("key");
    assertEquals(Optional.empty(), storage.get("key"));
  }
}
