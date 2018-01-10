package net.consensys.athena.impl.enclave.sodium;

import static junit.framework.TestCase.assertEquals;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.KeyConfig;
import net.consensys.athena.api.enclave.KeyStore;
import net.consensys.athena.impl.config.MemoryConfig;
import net.consensys.athena.impl.http.server.HttpContentType;
import net.consensys.athena.impl.utils.Base64;
import net.consensys.athena.impl.utils.Serializer;

import java.security.PublicKey;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

public class SodiumPublicKeyTest {
  private final KeyStore memoryKeyStore = new SodiumMemoryKeyStore();
  private KeyConfig keyConfig = new KeyConfig("ingore", Optional.empty());;

  private final MemoryConfig config = new MemoryConfig();
  private Enclave enclave;

  @Before
  public void setUp() throws Exception {
    config.setLibSodiumPath(LibSodiumSettings.defaultLibSodiumPath());
    enclave = new LibSodiumEnclave(config, memoryKeyStore);
  }

  @Test
  public void testRoundTripSerialization() {
    SodiumPublicKey key = new SodiumPublicKey("fake encoded".getBytes());
    Serializer serializer = new Serializer();
    byte[] bytes = serializer.serialize(HttpContentType.JSON, key);
    assertEquals(key, serializer.deserialize(HttpContentType.JSON, SodiumPublicKey.class, bytes));
    bytes = serializer.serialize(HttpContentType.CBOR, key);
    assertEquals(key, serializer.deserialize(HttpContentType.CBOR, SodiumPublicKey.class, bytes));
  }

  @Test
  public void testKeyFromB64EqualsOriginal() {
    // generate key
    PublicKey fakePK = memoryKeyStore.generateKeyPair(keyConfig);

    // b64 representation of key
    String b64 = Base64.encode(fakePK.getEncoded());

    // create new object from decoded key
    PublicKey rebuiltKey = new SodiumPublicKey(Base64.decode(b64));

    assertEquals(fakePK, rebuiltKey);
  }
}
