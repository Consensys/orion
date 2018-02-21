package net.consensys.athena.impl.storage.file;

import static org.junit.Assert.assertEquals;

import net.consensys.athena.impl.utils.Serializer;

import java.io.UnsupportedEncodingException;
import java.util.Optional;

import org.junit.After;
import org.junit.Test;

public class MapDbStorageTest {
  String path = "db";
  private Serializer serializer = new Serializer();
  MapDbStorage<String> storage = new MapDbStorage<>(String.class, path, serializer);

  final String toStore = "data";

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
    assertEquals(toStore, storage.get("key").get());
  }

  @Test
  public void testRetrieveWithoutStore() {
    assertEquals(Optional.empty(), storage.get("missing"));
  }

  @Test
  public void testStoreAndRetrieveAcrossSessions() {
    storage.put("key", toStore);
    storage.close();
    MapDbStorage<byte[]> secondStorage = new MapDbStorage(String.class, path, serializer);
    assertEquals(toStore, secondStorage.get("key").get());
  }

  @Test
  public void testStoreAndRemove() {
    storage.put("key", toStore);
    storage.remove("key");
    assertEquals(Optional.empty(), storage.get("key"));
  }
}
