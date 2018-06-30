package net.consensys.orion.impl.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.api.enclave.CombinedKey;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.enclave.KeyConfig;
import net.consensys.orion.api.enclave.KeyStore;
import net.consensys.orion.api.enclave.PublicKey;
import net.consensys.orion.impl.enclave.sodium.MemoryKeyStore;
import net.consensys.orion.impl.http.server.HttpContentType;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class SerializerTest {

  @Test
  void jsonSerialization() {
    DummyObject dummyObjectOriginal = new DummyObject();
    byte[] bytes = Serializer.serialize(HttpContentType.JSON, dummyObjectOriginal);
    DummyObject dummyObject = Serializer.deserialize(HttpContentType.JSON, DummyObject.class, bytes);
    assertEquals(dummyObjectOriginal, dummyObject);
  }

  @Test
  void cborSerialization() {
    DummyObject dummyObjectOriginal = new DummyObject();
    byte[] bytes = Serializer.serialize(HttpContentType.CBOR, dummyObjectOriginal);
    DummyObject dummyObject = Serializer.deserialize(HttpContentType.CBOR, DummyObject.class, bytes);
    assertEquals(dummyObjectOriginal, dummyObject);
  }

  @Test
  void sodiumEncryptedPayloadSerialization(@TempDirectory Path tempDir) {
    final KeyStore memoryKeyStore = new MemoryKeyStore();
    KeyConfig keyConfig = new KeyConfig(tempDir.resolve("ignore"), Optional.empty());

    CombinedKey[] combinedKeys = new CombinedKey[0];
    byte[] combinedKeyNonce = {};
    byte[] nonce = {};
    PublicKey sender = memoryKeyStore.generateKeyPair(keyConfig);

    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    EncryptedPayload original = new EncryptedPayload(sender, nonce, combinedKeyNonce, combinedKeys, toEncrypt);

    EncryptedPayload processed = Serializer.deserialize(
        HttpContentType.CBOR,
        EncryptedPayload.class,
        Serializer.serialize(HttpContentType.CBOR, original));

    assertEquals(original, processed);
  }

  static class DummyObject implements Serializable {
    public String name;
    public int age;

    DummyObject() {
      this.name = "john";
      this.age = 42;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      DummyObject that = (DummyObject) o;
      return age == that.age && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      int result = name.hashCode();
      result = 31 * result + age;
      return result;
    }
  }
}
