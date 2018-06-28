package net.consensys.orion.api.enclave;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.impl.enclave.sodium.MemoryKeyStore;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Base64;
import net.consensys.orion.impl.utils.Serializer;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class PublicKeyTest {

  private KeyConfig keyConfig;
  private KeyStore memoryKeyStore;

  @BeforeEach
  void setUp(@TempDirectory Path tempDir) {
    keyConfig = new KeyConfig(tempDir.resolve("ignore"), Optional.empty());
    memoryKeyStore = new MemoryKeyStore();
  }

  @Test
  void roundTripSerialization() {
    PublicKey key = new PublicKey("fake encoded".getBytes(UTF_8));
    byte[] bytes = Serializer.serialize(HttpContentType.JSON, key);
    assertEquals(key, Serializer.deserialize(HttpContentType.JSON, PublicKey.class, bytes));
    bytes = Serializer.serialize(HttpContentType.CBOR, key);
    assertEquals(key, Serializer.deserialize(HttpContentType.CBOR, PublicKey.class, bytes));
  }

  @Test
  void keyFromB64EqualsOriginal() {
    // generate key
    PublicKey fakePK = memoryKeyStore.generateKeyPair(keyConfig);

    // b64 representation of key
    String b64 = Base64.encode(fakePK.toBytes());

    // create new object from decoded key
    PublicKey rebuiltKey = new PublicKey(Base64.decode(b64));

    assertEquals(fakePK, rebuiltKey);
  }
}
