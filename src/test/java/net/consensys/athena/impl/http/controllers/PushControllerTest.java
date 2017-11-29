package net.consensys.athena.impl.http.controllers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Base64;
import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageKey;
import net.consensys.athena.api.storage.StorageKeyBuilder;
import net.consensys.athena.impl.enclave.BouncyCastleEnclave;
import net.consensys.athena.impl.storage.Sha512_256StorageKeyBuilder;
import net.consensys.athena.impl.storage.SimpleStorage;
import net.consensys.athena.impl.storage.file.MapDbStorage;

import java.util.Random;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.Test;

public class PushControllerTest {
  private static final Enclave enclave = new BouncyCastleEnclave();
  private static final StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder(enclave);
  private static final String DB_PATH = "push.db.test";
  private static final Storage storage = new MapDbStorage(DB_PATH, keyBuilder);

  private static final PushController controller = new PushController(storage);

  @Test
  public void testPayloadIsStored() throws Exception {
    // generate random byte content
    byte[] toCheck = new byte[342];
    new Random().nextBytes(toCheck);

    // create Netty content and http request object
    ByteBuf content = Unpooled.copiedBuffer(toCheck);
    FullHttpRequest req =
        new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/push", content);

    // call the controller with fake request / response
    FullHttpResponse defaultResponse =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    FullHttpResponse response = controller.handle(req, defaultResponse);

    // ensure we got a 200 OK back
    assertEquals(response.status().code(), HttpResponseStatus.OK.code());

    // get the key / digest from response, and build our key object
    String b64 = new String(response.content().array(), "UTF-8");
    StorageKey key = new SimpleStorage(b64);

    // retrieve stored value
    StorageData data = storage.retrieve(key);

    // ensure we fetched something
    assertNotNull(data);

    // ensure what was stored is what we sent
    assertArrayEquals(data.getRaw(), toCheck);
  }

  @Test
  public void testValidRequest() throws Exception {
    // generate random byte content
    byte[] toCheck = new byte[342];
    new Random().nextBytes(toCheck);

    // create Netty content and http request object
    ByteBuf content = Unpooled.copiedBuffer(toCheck);
    FullHttpRequest req =
        new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/push", content);

    // call the controller with fake request / response
    FullHttpResponse defaultResponse =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    FullHttpResponse response = controller.handle(req, defaultResponse);

    // ensure we didn't get a 200 OK : we provided GET HTTP METHOD instead of POST
    assertNotEquals(response.status().code(), HttpResponseStatus.OK.code());

    req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/push");
    response = controller.handle(req, defaultResponse);
    // ensure we didn't get a 200 OK : we provided an empty payload
    assertNotEquals(response.status().code(), HttpResponseStatus.OK.code());
  }
}
