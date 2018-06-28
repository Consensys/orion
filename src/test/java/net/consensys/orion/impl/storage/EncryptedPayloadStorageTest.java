package net.consensys.orion.impl.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.kv.MapKeyValueStore;
import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.storage.Storage;
import net.consensys.orion.api.storage.StorageKeyBuilder;
import net.consensys.orion.impl.enclave.sodium.SodiumEnclaveStub;

import java.security.Security;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;

class EncryptedPayloadStorageTest {

  static {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  private Enclave enclave = new SodiumEnclaveStub();
  private StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder();
  private Storage<EncryptedPayload> payloadStorage = new EncryptedPayloadStorage(new MapKeyValueStore(), keyBuilder);

  @Test
  void storeAndRetrieve() throws Exception {
    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    EncryptedPayload toStore = enclave.encrypt(toEncrypt, null, null);

    String key = payloadStorage.put(toStore).get();
    assertEquals(toStore, payloadStorage.get(key).get().get());
  }

  @Test
  void retrieveWithoutStore() throws Exception {
    assertEquals(Optional.empty(), payloadStorage.get("missing").get());
  }
}
