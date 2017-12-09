package net.consensys.athena.impl.storage.file;

import static org.junit.Assert.assertEquals;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageKey;
import net.consensys.athena.api.storage.StorageKeyBuilder;
import net.consensys.athena.impl.enclave.BouncyCastleEnclave;
import net.consensys.athena.impl.storage.Sha512_256StorageKeyBuilder;
import net.consensys.athena.impl.storage.SimpleStorage;

import java.util.Optional;

import org.junit.After;
import org.junit.Test;

public class MapDbStorageTest {
  private Enclave enclave = new BouncyCastleEnclave();
  private StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder(enclave);
  String path = "db";
  MapDbStorage storage = new MapDbStorage(path, keyBuilder);

  @After
  public void tearDown() {
    if (storage.isOpen()) {
      storage.close();
    }
  }

  @Test
  public void testStoreAndRetrieve() throws Exception {
    StorageData data = new SimpleStorage("hello".getBytes());
    StorageKey key = storage.store(data);
    assertEquals(data, storage.retrieve(key).get());
  }

  @Test
  public void testRetrieveWithoutStore() throws Exception {
    StorageKey key = new SimpleStorage("missing".getBytes());
    assertEquals(Optional.empty(), storage.retrieve(key));
  }

  @Test
  public void testStoreAndRetrieveAcrossSessions() throws Exception {
    StorageData data = new SimpleStorage("hello".getBytes());
    StorageKey key = storage.store(data);
    storage.close();
    MapDbStorage secondStorage = new MapDbStorage(path, keyBuilder);
    assertEquals(data, secondStorage.retrieve(key).get());
  }
}
