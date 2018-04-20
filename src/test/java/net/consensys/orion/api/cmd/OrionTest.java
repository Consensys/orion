package net.consensys.orion.api.cmd;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.api.exception.OrionException;
import net.consensys.orion.impl.config.MemoryConfig;
import net.consensys.orion.impl.enclave.sodium.storage.StoredPrivateKey;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Serializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import org.junit.Test;
import org.mockito.Mockito;

public class OrionTest {
  private Orion orion = new Orion();

  @Test
  public void loadSampleConfig() throws Exception {
    Config config = orion.loadConfig(Optional.of(Paths.get("src/main/resources/sample.conf")));
    assertEquals(8080, config.nodePort());
  }

  @Test
  public void defaultConfigIsUsedWhenNoneProvided() throws Exception {
    Config config = orion.loadConfig(Optional.empty());

    assertEquals(8080, config.nodePort());
  }

  @Test
  public void generateKeysWithArgumentProvided() throws Exception {
    //Test "--generatekeys" option
    String[] args1 = {"--generatekeys", "testkey1"};
    String input = "\n";
    InputStream in = new ByteArrayInputStream(input.getBytes(UTF_8));
    System.setIn(in);
    orion.run(System.out, System.err, args1);

    Path privateKey1 = Paths.get("testkey1.key");
    Path publicKey1 = Paths.get("testkey1.pub");
    assertTrue(Files.exists(privateKey1));
    assertTrue(Files.exists(publicKey1));
    Files.delete(privateKey1);
    Files.delete(publicKey1);

    //Test "-g" option and multiple key files
    args1 = new String[] {"-g", "testkey2,testkey3"};

    String input2 = "\n\n";
    InputStream in2 = new ByteArrayInputStream(input2.getBytes(UTF_8));
    System.setIn(in2);

    orion.run(System.out, System.err, args1);

    Path privateKey2 = Paths.get("testkey2.key");
    Path publicKey2 = Paths.get("testkey2.pub");
    Path privateKey3 = Paths.get("testkey3.key");
    Path publicKey3 = Paths.get("testkey3.pub");

    assertTrue(Files.exists(privateKey2));
    assertTrue(Files.exists(publicKey2));

    assertTrue(Files.exists(privateKey3));
    assertTrue(Files.exists(publicKey3));

    Files.delete(privateKey2);
    Files.delete(publicKey2);
    Files.delete(privateKey3);
    Files.delete(publicKey3);
  }

  @Test
  public void generateUnlockedKey() throws Exception {

    String[] args1 = {"--generatekeys", "testkey1"};
    String input = "\n";
    InputStream in = new ByteArrayInputStream(input.getBytes(UTF_8));
    System.setIn(in);
    orion.run(System.out, System.err, args1);

    Path privateKey1 = Paths.get("testkey1.key");
    Path publicKey1 = Paths.get("testkey1.pub");

    if (Files.exists(privateKey1)) {
      StoredPrivateKey storedPrivateKey =
          Serializer.readFile(HttpContentType.JSON, privateKey1, StoredPrivateKey.class);

      assertEquals(StoredPrivateKey.UNLOCKED, storedPrivateKey.type());

      Files.delete(privateKey1);
    } else {
      fail("Key was not created");
    }

    Files.delete(publicKey1);
  }

  @Test
  public void generateLockedKey() throws Exception {

    String[] args1 = {"--generatekeys", "testkey1"};
    String input = "abc\n";
    InputStream in = new ByteArrayInputStream(input.getBytes(UTF_8));
    System.setIn(in);
    orion.run(System.out, System.err, args1);

    Path privateKey1 = Paths.get("testkey1.key");
    Path publicKey1 = Paths.get("testkey1.pub");

    if (Files.exists(privateKey1)) {
      StoredPrivateKey storedPrivateKey =
          Serializer.readFile(HttpContentType.JSON, privateKey1, StoredPrivateKey.class);

      assertEquals(StoredPrivateKey.ARGON2_SBOX, storedPrivateKey.type());

      Files.delete(privateKey1);
    } else {
      fail("Key was not created");
    }

    Files.delete(publicKey1);
  }

  @Test
  public void missingConfigFile() {
    Orion orion = new Orion();
    try {
      orion.run(System.out, System.err, "someMissingFile.txt");
      fail();
    } catch (OrionException e) {
      assertEquals(OrionErrorCode.CONFIG_FILE_MISSING, e.code());
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void startupFails() throws IOException {
    Vertx vertx = Mockito.mock(Vertx.class);
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
    try {
      MemoryConfig config = new MemoryConfig();
      Path knownClientsFile = Files.createTempFile("knownclients", ".txt");
      config.setTlsKnownClients(knownClientsFile);
      config.setTlsKnownServers(Files.createTempFile("knownservers", ".txt"));
      config.setWorkDir(Files.createTempDirectory("work"));
      orion.run(System.out, System.err, config);
      fail();
    } catch (OrionStartException e) {
      assertEquals("Orion failed to start: Didn't work", e.getMessage());
    }
  }
}
