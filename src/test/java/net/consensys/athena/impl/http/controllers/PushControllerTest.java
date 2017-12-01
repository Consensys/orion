package net.consensys.athena.impl.http.controllers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageKey;
import net.consensys.athena.api.storage.StorageKeyBuilder;
import net.consensys.athena.impl.enclave.BouncyCastleEnclave;
import net.consensys.athena.impl.http.helpers.HttpTester;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.storage.Sha512_256StorageKeyBuilder;
import net.consensys.athena.impl.storage.SimpleStorage;
import net.consensys.athena.impl.storage.memory.MemoryStorage;

import java.util.Optional;
import java.util.Random;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Test;

public class PushControllerTest {

  private final Enclave enclave = new BouncyCastleEnclave();
  private final StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder(enclave);
  private final Storage storage = new MemoryStorage(keyBuilder);

  private final Controller controller = new PushController(storage);

  @Test
  public void testPayloadIsStored() throws Exception {
    // generate random byte content
    byte[] toCheck = new byte[342];
    new Random().nextBytes(toCheck);

    // perform fake http request
    FullHttpResponse response =
        new HttpTester(controller)
            .uri("/push")
            .method(HttpMethod.POST)
            .payload(toCheck)
            .sendRequest();

    // ensure we got a 200 OK back
    assertEquals(response.status().code(), HttpResponseStatus.OK.code());

    // get the key / digest from response, and build our key object
    String b64 = new String(response.content().array(), "UTF-8");
    StorageKey key = new SimpleStorage(b64);

    // retrieve stored value
    Optional<StorageData> data = storage.retrieve(key);

    // ensure we fetched something
    assert (data.isPresent());

    // ensure what was stored is what we sent
    assertArrayEquals(data.get().getRaw(), toCheck);
  }

  @Test
  public void testRequestHasPayload() throws Exception {
    // perform fake http request
    FullHttpResponse response =
        new HttpTester(controller).uri("/push").method(HttpMethod.POST).sendRequest();

    // ensure we didn't get a 200 OK : we provided an empty payload
    assertNotEquals(response.status().code(), HttpResponseStatus.OK.code());
  }

  @Test
  public void testRequestHasHttpPostMethod() throws Exception {
    // generate random byte content
    byte[] toCheck = new byte[342];
    new Random().nextBytes(toCheck);

    // perform fake http request
    FullHttpResponse response =
        new HttpTester(controller)
            .uri("/push")
            .method(HttpMethod.GET)
            .payload(toCheck)
            .sendRequest();

    // ensure we didn't get a 200 OK : we provided GET HTTP METHOD instead of POST
    assertNotEquals(response.status().code(), HttpResponseStatus.OK.code());
  }
}
