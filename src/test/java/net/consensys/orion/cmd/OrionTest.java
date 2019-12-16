/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.orion.cmd;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.orion.config.Config;
import net.consensys.orion.enclave.sodium.StoredPrivateKey;
import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.utils.Serializer;

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
import org.apache.tuweni.junit.TempDirectory;
import org.apache.tuweni.junit.TempDirectoryExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

@ExtendWith(TempDirectoryExtension.class)
class OrionTest {
  private final Orion orion = new Orion();

  @Test
  void generateUnlockedKeysWithArgumentProvided(@TempDirectory final Path tempDir) throws Exception {
    final Path key1 = tempDir.resolve("testkey1").toAbsolutePath();
    final Path privateKey1 = tempDir.resolve("testkey1.key");
    final Path publicKey1 = tempDir.resolve("testkey1.pub");

    // Test "--generatekeys" option
    final String[] args1 = {"--generatekeys", key1.toString()};
    final String input = "\n";
    final InputStream in = new ByteArrayInputStream(input.getBytes(UTF_8));
    System.setIn(in);
    orion.run(System.out, System.err, args1);

    assertTrue(Files.exists(privateKey1));
    assertTrue(Files.exists(publicKey1));

    final StoredPrivateKey storedPrivateKey =
        Serializer.readFile(HttpContentType.JSON, privateKey1, StoredPrivateKey.class);
    assertEquals(StoredPrivateKey.UNLOCKED, storedPrivateKey.type());

    Files.delete(privateKey1);
    Files.delete(publicKey1);
  }

  @Test
  void generateLockedKeysWithArgumentProvided(@TempDirectory final Path tempDir) throws Exception {
    final Path key1 = tempDir.resolve("testkey1").toAbsolutePath();
    final Path privateKey1 = tempDir.resolve("testkey1.key");
    final Path publicKey1 = tempDir.resolve("testkey1.pub");

    // Test "--generatekeys" option
    final String[] args1 = {"--generatekeys", key1.toString()};
    final String input = "abc\n";
    final InputStream in = new ByteArrayInputStream(input.getBytes(UTF_8));
    System.setIn(in);
    orion.run(System.out, System.err, args1);

    assertTrue(Files.exists(privateKey1));
    assertTrue(Files.exists(publicKey1));

    final StoredPrivateKey storedPrivateKey =
        Serializer.readFile(HttpContentType.JSON, privateKey1, StoredPrivateKey.class);
    assertEquals(StoredPrivateKey.ENCRYPTED, storedPrivateKey.type());

    Files.delete(privateKey1);
    Files.delete(publicKey1);
  }

  @Test
  void generateMultipleKeys(@TempDirectory final Path tempDir) throws Exception {
    final Path key1 = tempDir.resolve("testkey1").toAbsolutePath();
    final Path privateKey1 = tempDir.resolve("testkey1.key");
    final Path publicKey1 = tempDir.resolve("testkey1.pub");
    final Path key2 = tempDir.resolve("testkey2").toAbsolutePath();
    final Path privateKey2 = tempDir.resolve("testkey2.key");
    final Path publicKey2 = tempDir.resolve("testkey2.pub");

    //Test "-g" option and multiple key files
    final String[] args1 = new String[] {"-g", key1.toString() + "," + key2.toString()};

    final String input2 = "\n\n";
    final InputStream in2 = new ByteArrayInputStream(input2.getBytes(UTF_8));
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
    final Orion orion = new Orion();
    final OrionStartException e =
        assertThrows(OrionStartException.class, () -> orion.run(System.out, System.err, "someMissingFile.txt"));
    assertTrue(e.getMessage().startsWith("Could not open '"));
  }

  @SuppressWarnings("unchecked")
  @Test
  void startupFails(@TempDirectory final Path tempDir) {
    final VertxInternal vertx = Mockito.mock(VertxInternal.class);
    final HttpServer httpServer = Mockito.mock(HttpServer.class);
    Mockito.when(vertx.createHttpServer(Mockito.any())).thenReturn(httpServer);
    Mockito.when(httpServer.requestHandler(Mockito.any())).thenReturn(httpServer);
    Mockito.when(httpServer.exceptionHandler(Mockito.any())).thenReturn(httpServer);
    Mockito.when(httpServer.listen(Mockito.anyObject())).then(answer -> {
      final Handler<AsyncResult<HttpServer>> handler = answer.getArgumentAt(0, Handler.class);
      handler.handle(Future.failedFuture("Didn't work"));
      return httpServer;
    });
    Mockito.doAnswer(answer -> {
      Handler<AsyncResult<HttpServer>> handler = answer.getArgumentAt(1, Handler.class);
      handler.handle(Future.failedFuture("Didn't work"));
      return null;
    }).when(vertx).deployVerticle(Mockito.any(Verticle.class), Mockito.any(Handler.class));

    final Orion orion = new Orion(vertx);
    final Config config = Config.load("workdir=\"" + tempDir.resolve("data") + "\"\ntls=\"off\"\n");
    assertThrows(OrionStartException.class, () -> orion.run(System.out, System.err, config));
  }
}
