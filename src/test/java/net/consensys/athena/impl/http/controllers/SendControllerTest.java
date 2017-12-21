package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageKeyBuilder;
import net.consensys.athena.impl.enclave.CesarEnclave;
import net.consensys.athena.impl.http.controllers.SendController.SendRequest;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.RequestImpl;
import net.consensys.athena.impl.http.data.Serializer;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.network.MemoryNetworkNodes;
import net.consensys.athena.impl.storage.EncryptedPayloadStorage;
import net.consensys.athena.impl.storage.Sha512_256StorageKeyBuilder;
import net.consensys.athena.impl.storage.memory.MemoryStorage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import org.junit.Test;

public class SendControllerTest {
  private final Enclave enclave = new CesarEnclave();

  private final StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder(enclave);

  private final Storage<EncryptedPayload> storage =
      new EncryptedPayloadStorage(new MemoryStorage(), keyBuilder);

  private final Serializer serializer =
      new Serializer(new ObjectMapper(), new ObjectMapper(new CBORFactory()));

  private final NetworkNodes networkNodes = new MemoryNetworkNodes();

  private final Controller controller =
      new SendController(enclave, storage, ContentType.JSON, networkNodes, serializer);

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidRequest() {
    SendRequest sendRequest = new SendRequest();
    sendRequest.from = "me";
    controller.handle(new RequestImpl(sendRequest));
  }
}
