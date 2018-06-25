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
  void tearDown() {
    storage.close();
  }

  @Test
  public void storeAndRetrieve() throws Exception {
    storage.put("key", "data").join();
    assertEquals("data", storage.get("key").get().get());
  }

  @Test
  void retrieveWithoutStore() throws Exception {
    assertEquals(Optional.empty(), storage.get("missing").get());
  }

  @Test
  void storeAndRetrieveAcrossSessions(@TempDirectory Path tempDir) throws Exception {
    storage.put("key", "data").join();
    storage.close();
    try (MapDbStorage<String> secondStorage = new MapDbStorage<>(String.class, tempDir)) {
      assertEquals("data", secondStorage.get("key").get().get());
    }
  }
}
