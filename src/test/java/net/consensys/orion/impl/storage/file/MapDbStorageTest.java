package net.consensys.orion.impl.storage.file;

import static java.nio.file.Files.createTempDirectory;
import static net.consensys.util.Files.deleteRecursively;
import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MapDbStorageTest {

  private Path tempDir;
  private MapDbStorage<String> storage;

  @Before
  public void setup() throws Exception {
    tempDir = createTempDirectory(this.getClass().getSimpleName() + "-data");
    storage = new MapDbStorage<>(String.class, tempDir);
  }

  @After
  public void teardown() throws Exception {
    if (storage.isOpen()) {
      storage.close();
    }
    deleteRecursively(tempDir);
  }

  @After
  public void tearDown() {
    if (storage.isOpen()) {
      storage.close();
    }
  }

  @Test
  public void storeAndRetrieve() {
    storage.put("key", "data");
    assertEquals("data", storage.get("key").get());
  }

  @Test
  public void retrieveWithoutStore() {
    assertEquals(Optional.empty(), storage.get("missing"));
  }

  @Test
  public void storeAndRetrieveAcrossSessions() {
    storage.put("key", "data");
    storage.close();
    MapDbStorage<String> secondStorage = new MapDbStorage<>(String.class, tempDir);
    assertEquals("data", secondStorage.get("key").get());
  }

  @Test
  public void storeAndRemove() {
    storage.put("key", "data");
    storage.remove("key");
    assertEquals(Optional.empty(), storage.get("key"));
  }
}
