package net.consensys.orion.api.cmd;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.api.exception.OrionException;
import net.consensys.orion.impl.enclave.sodium.storage.StoredPrivateKey;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Serializer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
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
    Config config = orion.loadConfig(Optional.of(new File("src/main/resources/sample.conf")));
    assertEquals(8080, config.port());

    File expectedSocket = new File("data/orion.ipc");
    assertTrue(config.socket().isPresent());
    assertEquals(expectedSocket, config.socket().get());
  }

  @Test
  public void defaultConfigIsUsedWhenNoneProvided() throws Exception {
    Config config = orion.loadConfig(Optional.empty());

    assertEquals(8080, config.port());
    assertFalse(config.socket().isPresent());
  }

  @Test
  public void generateKeysWithArgumentProvided() throws Exception {
    //Test "--generatekeys" option
    String[] args1 = {"--generatekeys", "testkey1"};
    String input = "\n";
    InputStream in = new ByteArrayInputStream(input.getBytes(UTF_8));
    System.setIn(in);
    orion.run(System.out, System.err, args1);

    File privateKey1 = new File("testkey1.key");
    File publicKey1 = new File("testkey1.pub");
    assertTrue(privateKey1.exists());
    assertTrue(publicKey1.exists());
    if (privateKey1.exists()) {
      privateKey1.delete();
    }
    if (publicKey1.exists()) {
      publicKey1.delete();
    }

    //Test "-g" option and multiple key files
    args1 = new String[] {"-g", "testkey2,testkey3"};

    String input2 = "\n\n";
    InputStream in2 = new ByteArrayInputStream(input2.getBytes(UTF_8));
    System.setIn(in2);

    orion.run(System.out, System.err, args1);

    File privateKey2 = new File("testkey2.key");
    File publicKey2 = new File("testkey2.pub");
    File privateKey3 = new File("testkey3.key");
    File publicKey3 = new File("testkey3.pub");

    assertTrue(privateKey2.exists());
    assertTrue(publicKey2.exists());

    assertTrue(privateKey3.exists());
    assertTrue(publicKey3.exists());

    if (privateKey2.exists()) {
      privateKey2.delete();
    }
    if (publicKey2.exists()) {
      publicKey2.delete();
    }
    if (privateKey3.exists()) {
      privateKey3.delete();
    }
    if (publicKey3.exists()) {
      publicKey3.delete();
    }
  }

  @Test
  public void generateUnlockedKey() throws Exception {

    String[] args1 = {"--generatekeys", "testkey1"};
    String input = "\n";
    InputStream in = new ByteArrayInputStream(input.getBytes(UTF_8));
    System.setIn(in);
    orion.run(System.out, System.err, args1);

    File privateKey1 = new File("testkey1.key");
    File publicKey1 = new File("testkey1.pub");

    if (privateKey1.exists()) {
      StoredPrivateKey storedPrivateKey =
          Serializer.readFile(HttpContentType.JSON, privateKey1, StoredPrivateKey.class);

      assertEquals(StoredPrivateKey.UNLOCKED, storedPrivateKey.type());

      privateKey1.delete();
    } else {
      fail("Key was not created");
    }

    if (publicKey1.exists()) {
      publicKey1.delete();
    }
  }

  @Test
  public void generateLockedKey() throws Exception {

    String[] args1 = {"--generatekeys", "testkey1"};
    String input = "abc\n";
    InputStream in = new ByteArrayInputStream(input.getBytes(UTF_8));
    System.setIn(in);
    orion.run(System.out, System.err, args1);

    File privateKey1 = new File("testkey1.key");
    File publicKey1 = new File("testkey1.pub");

    if (privateKey1.exists()) {
      StoredPrivateKey storedPrivateKey =
          Serializer.readFile(HttpContentType.JSON, privateKey1, StoredPrivateKey.class);

      assertEquals(StoredPrivateKey.ARGON2_SBOX, storedPrivateKey.type());

      privateKey1.delete();
    } else {
      fail("Key was not created");
    }

    if (publicKey1.exists()) {
      publicKey1.delete();
    }
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
  public void startupFails() {
    Vertx vertx = Mockito.mock(Vertx.class);
    HttpServer httpServer = Mockito.mock(HttpServer.class);
    Mockito.when(vertx.createHttpServer(Mockito.any())).thenReturn(httpServer);
    Mockito.when(httpServer.requestHandler(Mockito.any())).thenReturn(httpServer);
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
      orion.run(System.out, System.err);
      fail();
    } catch (OrionException e) {
      assertEquals(OrionErrorCode.SERVICE_START_ERROR, e.code());
      assertEquals("Didn't work", e.getMessage());
    }
  }
}
