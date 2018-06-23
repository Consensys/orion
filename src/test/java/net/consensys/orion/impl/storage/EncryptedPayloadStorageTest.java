package net.consensys.orion.impl.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.storage.Storage;
import net.consensys.orion.api.storage.StorageKeyBuilder;
import net.consensys.orion.impl.enclave.sodium.LibSodiumEnclaveStub;
import net.consensys.orion.impl.storage.memory.MemoryStorage;

import java.security.Security;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;

class EncryptedPayloadStorageTest {

  static {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  private Enclave enclave = new LibSodiumEnclaveStub();
  private StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder();
  private MemoryStorage<EncryptedPayload> memory = new MemoryStorage<>();
  private Storage<EncryptedPayload> storage = new EncryptedPayloadStorage(memory, keyBuilder);

  @Test
  void storeAndRetrieve() {
    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    EncryptedPayload toStore = enclave.encrypt(toEncrypt, null, null);

    String key = storage.put(toStore);
    assertEquals(toStore, storage.get(key).get());
  }

  @Test
  void retrieveWithoutStore() {
    assertEquals(Optional.empty(), storage.get("missing"));
  }
}
