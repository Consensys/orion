package net.consensys.athena.impl.http.controllers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageKey;
import net.consensys.athena.api.storage.StorageKeyBuilder;
import net.consensys.athena.impl.enclave.CesarEnclave;
import net.consensys.athena.impl.http.controllers.ReceiveController.ReceiveRequest;
import net.consensys.athena.impl.http.controllers.ReceiveController.ReceiveResponse;
import net.consensys.athena.impl.http.helpers.HttpTester;
import net.consensys.athena.impl.http.server.ContentType;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Result;
import net.consensys.athena.impl.http.server.Serializer;
import net.consensys.athena.impl.storage.Sha512_256StorageKeyBuilder;
import net.consensys.athena.impl.storage.SimpleStorage;
import net.consensys.athena.impl.storage.memory.MemoryStorage;

import java.util.Base64;
import java.util.Random;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Test;

public class ReceiveControllerTest {
  private final Enclave enclave = new CesarEnclave();
  private final StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder(enclave);
  private final Storage storage = new MemoryStorage(keyBuilder);

  private final Controller receiveController =
      new ReceiveController(enclave, storage, ContentType.JSON);

  @Test
  public void testPayloadIsRetrieved() throws Exception {
    // let's create a random payload
    byte[] toCheck = new byte[342];
    new Random().nextBytes(toCheck);

    // encrypt the payload
    EncryptedPayload encryptedPayload = enclave.encrypt(toCheck, null, null);

    // serialize it
    byte[] serialized = Serializer.serialize(encryptedPayload, ContentType.JAVA_ENCODED);

    // store it
    StorageKey key = storage.store(new SimpleStorage(serialized));

    // and try to retrieve it with the receiveController
    ReceiveRequest req = new ReceiveRequest(key.getBase64Encoded(), null);

    // perform fake http request
    Result result =
        new HttpTester(receiveController)
            .uri("/receive")
            .method(HttpMethod.POST)
            .payload(Serializer.serialize(req, ContentType.JSON))
            .sendRequest();

    // ensure we got a 200 OK back
    assertEquals(result.getStatus().code(), HttpResponseStatus.OK.code());

    // ensure result has a payload
    assert (result.getPayload().isPresent());

    // read response
    ReceiveResponse response = (ReceiveResponse) result.getPayload().get();

    // ensure we got the decrypted response back
    assertArrayEquals(Base64.getDecoder().decode(response.payload), toCheck);
  }
}
