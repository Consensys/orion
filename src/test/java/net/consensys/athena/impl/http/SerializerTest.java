package net.consensys.athena.impl.http;

import static junit.framework.TestCase.assertEquals;

import net.consensys.athena.api.enclave.CombinedKey;
import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.KeyConfig;
import net.consensys.athena.api.enclave.KeyStore;
import net.consensys.athena.impl.config.MemoryConfig;
import net.consensys.athena.impl.enclave.SimpleEncryptedPayload;
import net.consensys.athena.impl.enclave.sodium.LibSodiumEnclave;
import net.consensys.athena.impl.enclave.sodium.LibSodiumSettings;
import net.consensys.athena.impl.enclave.sodium.SodiumMemoryKeyStore;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Serializer;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import org.junit.Test;

public class SerializerTest {

  final Serializer serializer = new Serializer();

  @Test
  public void testJsonSerialization() throws Exception {
    DummyObject dummyObjectOriginal = new DummyObject();
    byte[] bytes = serializer.serialize(ContentType.JSON, dummyObjectOriginal);
    DummyObject dummyObject = serializer.deserialize(ContentType.JSON, DummyObject.class, bytes);
    assert (dummyObject.equals(dummyObjectOriginal));
  }

  @Test
  public void testCBORSerialization() throws Exception {
    DummyObject dummyObjectOriginal = new DummyObject();
    byte[] bytes = serializer.serialize(ContentType.CBOR, dummyObjectOriginal);
    DummyObject dummyObject = serializer.deserialize(ContentType.CBOR, DummyObject.class, bytes);
    assert (dummyObject.equals(dummyObjectOriginal));
  }

  @Test
  public void testSimpleEncryptedPayloadSerialization() throws Exception {
    MemoryConfig config = new MemoryConfig();
    config.setLibSodiumPath(LibSodiumSettings.defaultLibSodiumPath());
    final KeyStore memoryKeyStore = new SodiumMemoryKeyStore();
    KeyConfig keyConfig = new KeyConfig("ignore", Optional.empty());
    ;
    Enclave enclave = new LibSodiumEnclave(config, memoryKeyStore);

    CombinedKey[] combinedKeys = new CombinedKey[0];
    byte[] combinedKeyNonce = {};
    byte[] nonce = {};
    PublicKey sender = memoryKeyStore.generateKeyPair(keyConfig);

    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    SimpleEncryptedPayload original =
        new SimpleEncryptedPayload(
            sender, nonce, combinedKeyNonce, combinedKeys, toEncrypt, new HashMap<>());

    SimpleEncryptedPayload processed =
        serializer.deserialize(
            ContentType.CBOR,
            SimpleEncryptedPayload.class,
            serializer.serialize(ContentType.CBOR, original));

    assertEquals(original, processed);
  }
}

class DummyObject implements Serializable {
  public String name;
  public int age;

  public DummyObject() {
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
}
