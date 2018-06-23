package net.consensys.orion.impl.storage.leveldb;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.impl.enclave.sodium.SodiumCombinedKey;
import net.consensys.orion.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.orion.impl.enclave.sodium.SodiumPublicKey;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class LevelDbStorageTest {

  @Test
  void itemThatIsPutCanBeRetrievedWithGet(@TempDirectory Path tempDir) {
    LevelDbStorage<SodiumEncryptedPayload> storage = new LevelDbStorage<>(SodiumEncryptedPayload.class, tempDir);
    try {
      SodiumPublicKey sender = new SodiumPublicKey("fake key".getBytes(UTF_8));
      byte[] nonce = "nonce".getBytes(UTF_8);
      byte[] ckNonce = "combined nonce".getBytes(UTF_8);
      SodiumCombinedKey[] keys = new SodiumCombinedKey[] {new SodiumCombinedKey("recipient".getBytes(UTF_8))};
      byte[] cipherText = "encrypted".getBytes(UTF_8);
      SodiumEncryptedPayload payload = new SodiumEncryptedPayload(sender, nonce, ckNonce, keys, cipherText);
      storage.put("key", payload);
      Optional<SodiumEncryptedPayload> fromDB = storage.get("key");
      assertEquals(payload, fromDB.get());
    } finally {
      storage.close();
    }
  }
}
