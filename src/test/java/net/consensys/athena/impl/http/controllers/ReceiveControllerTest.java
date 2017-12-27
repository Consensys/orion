package net.consensys.athena.impl.http.controllers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageKeyBuilder;
import net.consensys.athena.impl.helpers.CesarEnclave;
import net.consensys.athena.impl.http.controllers.ReceiveController.ReceiveRequest;
import net.consensys.athena.impl.http.controllers.ReceiveController.ReceiveResponse;
import net.consensys.athena.impl.http.data.ApiError;
import net.consensys.athena.impl.http.data.Base64;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Request;
import net.consensys.athena.impl.http.data.RequestImpl;
import net.consensys.athena.impl.http.data.Result;
import net.consensys.athena.impl.http.data.Serializer;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.storage.EncryptedPayloadStorage;
import net.consensys.athena.impl.storage.Sha512_256StorageKeyBuilder;
import net.consensys.athena.impl.storage.memory.MemoryStorage;

import java.util.Random;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Test;

public class ReceiveControllerTest {
  private final Enclave enclave = new CesarEnclave();
  private final StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder(enclave);
  private final Storage<EncryptedPayload> storage =
      new EncryptedPayloadStorage(new MemoryStorage(), keyBuilder);
  private final Serializer serializer = new Serializer();
  private final Controller receiveController =
      new ReceiveController(enclave, storage, ContentType.JSON, serializer);

  @Test
  public void testPayloadIsRetrieved() throws Exception {
    // generate random byte content
    byte[] toEncrypt = new byte[342];
    new Random().nextBytes(toEncrypt);

    // generate dummy encrypted payload object
    EncryptedPayload encryptedPayload = enclave.encrypt(toEncrypt, null, null);

    // store it
    String key = storage.put(encryptedPayload);

    // and try to retrieve it with the receiveController
    ReceiveRequest req = new ReceiveRequest(key, null);

    // submit request to controller
    Request controllerRequest = new RequestImpl(req);
    Result result = receiveController.handle(controllerRequest);

    // ensure we got a 200 OK back
    assertEquals(result.getStatus().code(), HttpResponseStatus.OK.code());

    // ensure result has a payload
    assert (result.getPayload().isPresent());

    // read response
    ReceiveResponse response = (ReceiveResponse) result.getPayload().get();

    // ensure we got the decrypted response back
    assertArrayEquals(Base64.decode(response.payload), toEncrypt);
  }

  @Test
  public void testResponseWhenKeyNotFound() throws Exception {
    ReceiveRequest req = new ReceiveRequest("notForMe", null);

    Result result = receiveController.handle(new RequestImpl(req));

    assertEquals(result.getStatus().code(), HttpResponseStatus.NOT_FOUND.code());

    assertEquals(result.getPayload().get(), new ApiError("Error: unable to retrieve payload"));
  }
}
