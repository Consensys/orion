package net.consensys.orion.impl.storage.file;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class MapDbStorageTest {

  private MapDbStorage<String> storage;

  @BeforeEach
  void setup(@TempDirectory Path tempDir) {
    storage = new MapDbStorage<>(String.class, tempDir);
  }

  @AfterEach
  void teardown() {
    if (storage.isOpen()) {
      storage.close();
    }
  }

  @AfterEach
  void tearDown() {
    if (storage.isOpen()) {
      storage.close();
    }
  }

  @Test
  void storeAndRetrieve() {
    storage.put("key", "data");
    assertEquals("data", storage.get("key").get());
  }

  @Test
  void retrieveWithoutStore() {
    assertEquals(Optional.empty(), storage.get("missing"));
  }

  @Test
  void storeAndRetrieveAcrossSessions(@TempDirectory Path tempDir) {
    storage.put("key", "data");
    storage.close();
    MapDbStorage<String> secondStorage = new MapDbStorage<>(String.class, tempDir);
    assertEquals("data", secondStorage.get("key").get());
  }

  @Test
  void storeAndRemove() {
    storage.put("key", "data");
    storage.remove("key");
    assertEquals(Optional.empty(), storage.get("key"));
  }
}
