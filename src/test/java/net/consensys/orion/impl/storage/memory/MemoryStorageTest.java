package net.consensys.orion.impl.storage.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class MemoryStorageTest {
  private MemoryStorage<String> storage = new MemoryStorage<>();

  @Test
  void storeAndRetrieve() throws InterruptedException {
    storage.put("key", "data");
    assertEquals("data", storage.get("key").get().get());
  }

  @Test
  void retrieveWithoutStore() throws InterruptedException {
    assertEquals(Optional.empty(), storage.get("missing").get());
  }
}
