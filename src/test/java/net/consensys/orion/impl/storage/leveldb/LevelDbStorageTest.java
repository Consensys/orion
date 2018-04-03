package net.consensys.orion.impl.storage.leveldb;

import static java.nio.file.Files.createTempDirectory;
import static net.consensys.util.Files.deleteRecursively;
import static org.junit.Assert.assertEquals;

import net.consensys.orion.impl.enclave.sodium.SodiumCombinedKey;
import net.consensys.orion.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.orion.impl.enclave.sodium.SodiumPublicKey;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LevelDbStorageTest {

  private static Path tempDir;

  @Before
  public void init() throws Exception {
    tempDir = createTempDirectory(LevelDbStorageTest.class.getSimpleName() + "-data");
  }

  @After
  public void cleanup() throws Exception {
    deleteRecursively(tempDir);
  }

  @Test
  public void itemThatIsPutCanBeRetrievedWithGet() throws Exception {
    LevelDbStorage<SodiumEncryptedPayload> storage =
        new LevelDbStorage<>(SodiumEncryptedPayload.class, tempDir.toString());
    try {
      SodiumPublicKey sender = new SodiumPublicKey("fake key".getBytes());
      byte[] nonce = "nonce".getBytes();
      byte[] ckNonce = "combined nonce".getBytes();
      SodiumCombinedKey[] keys = new SodiumCombinedKey[] {new SodiumCombinedKey("recipient".getBytes())};
      byte[] cipherText = "encrypted".getBytes();
      SodiumEncryptedPayload payload = new SodiumEncryptedPayload(sender, nonce, ckNonce, keys, cipherText);
      storage.put("key", payload);
      Optional<SodiumEncryptedPayload> fromDB = storage.get("key");
      assertEquals(payload, fromDB.get());
    } finally {
      storage.close();
    }
  }
}
