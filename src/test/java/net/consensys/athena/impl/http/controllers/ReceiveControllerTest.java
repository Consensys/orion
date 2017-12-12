package net.consensys.athena.impl.http.controllers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageId;
import net.consensys.athena.api.storage.StorageIdBuilder;
import net.consensys.athena.impl.enclave.CesarEnclave;
import net.consensys.athena.impl.http.controllers.ReceiveController.ReceiveRequest;
import net.consensys.athena.impl.http.controllers.ReceiveController.ReceiveResponse;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Request;
import net.consensys.athena.impl.http.data.RequestImpl;
import net.consensys.athena.impl.http.data.Result;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Serializer;
import net.consensys.athena.impl.storage.Sha512_256StorageIdBuilder;
import net.consensys.athena.impl.storage.SimpleStorage;
import net.consensys.athena.impl.storage.StorageKeyValueStorageDelegate;
import net.consensys.athena.impl.storage.memory.MemoryStorage;

import java.util.Base64;
import java.util.Optional;
import java.util.Random;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Test;

public class ReceiveControllerTest {
  private final Enclave enclave = new CesarEnclave();
  private final StorageIdBuilder keyBuilder = new Sha512_256StorageIdBuilder(enclave);
  private final Storage storage =
      new StorageKeyValueStorageDelegate(new MemoryStorage(), keyBuilder);
  private final Serializer serializer =
      new Serializer(new ObjectMapper(), new ObjectMapper(new CBORFactory()));
  private final Controller receiveController =
      new ReceiveController(enclave, storage, ContentType.JSON, serializer);

  @Test
  public void testPayloadIsRetrieved() throws Exception {
    // let's create a random payload
    byte[] toCheck = new byte[342];
    new Random().nextBytes(toCheck);

    // encrypt the payload
    EncryptedPayload encryptedPayload = enclave.encrypt(toCheck, null, null);

    // serialize it
    byte[] serialized = serializer.serialize(encryptedPayload, ContentType.JAVA_ENCODED);

    // store it
    StorageId id = storage.put(new SimpleStorage(serialized));

    // and try to retrieve it with the receiveController
    ReceiveRequest req = new ReceiveRequest(id.getBase64Encoded(), null);

    // submit request to controller
    Request controllerRequest = new RequestImpl(Optional.of(req));
    Result result = receiveController.handle(controllerRequest);

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
