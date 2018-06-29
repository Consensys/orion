package net.consensys.orion.api.cmd;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.api.exception.OrionException;
import net.consensys.orion.impl.enclave.sodium.storage.StoredPrivateKey;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Serializer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.VertxInternal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

@ExtendWith(TempDirectoryExtension.class)
class OrionTest {
  private Orion orion = new Orion();

  @Test
  void generateUnlockedKeysWithArgumentProvided(@TempDirectory Path tempDir) throws Exception {
    Path key1 = tempDir.resolve("testkey1").toAbsolutePath();
    Path privateKey1 = tempDir.resolve("testkey1.key");
    Path publicKey1 = tempDir.resolve("testkey1.pub");

    // Test "--generatekeys" option
    String[] args1 = {"--generatekeys", key1.toString()};
    String input = "\n";
    InputStream in = new ByteArrayInputStream(input.getBytes(UTF_8));
    System.setIn(in);
    orion.run(System.out, System.err, args1);

    assertTrue(Files.exists(privateKey1));
    assertTrue(Files.exists(publicKey1));

    StoredPrivateKey storedPrivateKey = Serializer.readFile(HttpContentType.JSON, privateKey1, StoredPrivateKey.class);
    assertEquals(StoredPrivateKey.UNLOCKED, storedPrivateKey.type());

    Files.delete(privateKey1);
    Files.delete(publicKey1);
  }

  @Test
  void generateLockedKeysWithArgumentProvided(@TempDirectory Path tempDir) throws Exception {
    Path key1 = tempDir.resolve("testkey1").toAbsolutePath();
    Path privateKey1 = tempDir.resolve("testkey1.key");
    Path publicKey1 = tempDir.resolve("testkey1.pub");

    // Test "--generatekeys" option
    String[] args1 = {"--generatekeys", key1.toString()};
    String input = "abc\n";
    InputStream in = new ByteArrayInputStream(input.getBytes(UTF_8));
    System.setIn(in);
    orion.run(System.out, System.err, args1);

    assertTrue(Files.exists(privateKey1));
    assertTrue(Files.exists(publicKey1));

    StoredPrivateKey storedPrivateKey = Serializer.readFile(HttpContentType.JSON, privateKey1, StoredPrivateKey.class);
    assertEquals(StoredPrivateKey.ARGON2_SBOX, storedPrivateKey.type());

    Files.delete(privateKey1);
    Files.delete(publicKey1);
  }

  @Test
  void generateMultipleKeys(@TempDirectory Path tempDir) throws Exception {
    Path key1 = tempDir.resolve("testkey1").toAbsolutePath();
    Path privateKey1 = tempDir.resolve("testkey1.key");
    Path publicKey1 = tempDir.resolve("testkey1.pub");
    Path key2 = tempDir.resolve("testkey2").toAbsolutePath();
    Path privateKey2 = tempDir.resolve("testkey2.key");
    Path publicKey2 = tempDir.resolve("testkey2.pub");

    //Test "-g" option and multiple key files
    String[] args1 = new String[] {"-g", key1.toString() + "," + key2.toString()};

    String input2 = "\n\n";
    InputStream in2 = new ByteArrayInputStream(input2.getBytes(UTF_8));
    System.setIn(in2);

    orion.run(System.out, System.err, args1);

    assertTrue(Files.exists(privateKey1));
    assertTrue(Files.exists(publicKey1));

    assertTrue(Files.exists(privateKey2));
    assertTrue(Files.exists(publicKey2));

    Files.delete(privateKey1);
    Files.delete(publicKey1);
    Files.delete(privateKey2);
    Files.delete(publicKey2);
  }

  @Test
  void missingConfigFile() {
    Orion orion = new Orion();
    OrionException e =
        assertThrows(OrionException.class, () -> orion.run(System.out, System.err, "someMissingFile.txt"));
    assertEquals(OrionErrorCode.CONFIG_FILE_MISSING, e.code());
  }

  @SuppressWarnings("unchecked")
  @Test
  void startupFails(@TempDirectory Path tempDir) {
    VertxInternal vertx = Mockito.mock(VertxInternal.class);
    HttpServer httpServer = Mockito.mock(HttpServer.class);
    Mockito.when(vertx.createHttpServer(Mockito.any())).thenReturn(httpServer);
    Mockito.when(httpServer.requestHandler(Mockito.any())).thenReturn(httpServer);
    Mockito.when(httpServer.exceptionHandler(Mockito.any())).thenReturn(httpServer);
    Mockito.when(httpServer.listen(Mockito.anyObject())).then(answer -> {
      Handler<AsyncResult<HttpServer>> handler = answer.getArgumentAt(0, Handler.class);
      handler.handle(Future.failedFuture("Didn't work"));
      return httpServer;
    });
    Mockito.doAnswer(answer -> {
      Handler<AsyncResult<HttpServer>> handler = answer.getArgumentAt(1, Handler.class);
      handler.handle(Future.failedFuture("Didn't work"));
      return null;
    }).when(vertx).deployVerticle(Mockito.any(Verticle.class), Mockito.any(Handler.class));

    Orion orion = new Orion(vertx);
    Config config = Config.load("workdir=\"" + tempDir.resolve("data") + "\"\ntls=\"off\"\n");
    assertThrows(OrionStartException.class, () -> orion.run(System.out, System.err, config));
  }
}
