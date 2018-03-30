package net.consensys.orion.impl.storage.leveldb;

import static org.junit.Assert.assertEquals;

import net.consensys.orion.impl.enclave.sodium.SodiumCombinedKey;
import net.consensys.orion.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.orion.impl.enclave.sodium.SodiumPublicKey;
import net.consensys.orion.impl.utils.Serializer;

import java.io.File;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;

import org.junit.Test;

public class LevelDbStorageTest {

  @Test
  public void itemThatIsPutCanBeRetrievedWithGet() throws Exception {
    String path = "level-db-test";
    LevelDbStorage<SodiumEncryptedPayload> storage =
        new LevelDbStorage<>(SodiumEncryptedPayload.class, path, new Serializer());
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
      Path rootPath = Paths.get(path);
      Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(
          File::delete);
    }
  }
}
