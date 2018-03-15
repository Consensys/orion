package net.consensys.orion.impl.storage.memory;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.junit.Test;

public class MemoryStorageTest {
  MemoryStorage<String> storage = new MemoryStorage();

  @Test
  public void storeAndRetrieve() {
    storage.put("key", "data");
    assertEquals("data", storage.get("key").get());
  }

  @Test
  public void retrieveWithoutStore() {
    assertEquals(Optional.empty(), storage.get("missing"));
  }
}
