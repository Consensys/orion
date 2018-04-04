package net.consensys.orion.impl.enclave.sodium;

import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.TestCase.assertEquals;

import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.KeyConfig;
import net.consensys.orion.api.enclave.KeyStore;
import net.consensys.orion.impl.config.MemoryConfig;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Base64;
import net.consensys.orion.impl.utils.Serializer;

import java.security.PublicKey;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

public class SodiumPublicKeyTest {
  private final MemoryConfig config = new MemoryConfig();

  private KeyConfig keyConfig = new KeyConfig("ignore", Optional.empty());
  private KeyStore memoryKeyStore;
  private Enclave enclave;

  @Before
  public void setUp() {
    config.setLibSodiumPath(LibSodiumSettings.defaultLibSodiumPath());
    memoryKeyStore = new SodiumMemoryKeyStore(config);
    enclave = new LibSodiumEnclave(config, memoryKeyStore);
  }

  @Test
  public void roundTripSerialization() {
    SodiumPublicKey key = new SodiumPublicKey("fake encoded".getBytes(UTF_8));
    byte[] bytes = Serializer.serialize(HttpContentType.JSON, key);
    assertEquals(key, Serializer.deserialize(HttpContentType.JSON, SodiumPublicKey.class, bytes));
    bytes = Serializer.serialize(HttpContentType.CBOR, key);
    assertEquals(key, Serializer.deserialize(HttpContentType.CBOR, SodiumPublicKey.class, bytes));
  }

  @Test
  public void keyFromB64EqualsOriginal() {
    // generate key
    PublicKey fakePK = memoryKeyStore.generateKeyPair(keyConfig);

    // b64 representation of key
    String b64 = Base64.encode(fakePK.getEncoded());

    // create new object from decoded key
    PublicKey rebuiltKey = new SodiumPublicKey(Base64.decode(b64));

    assertEquals(fakePK, rebuiltKey);
  }
}
