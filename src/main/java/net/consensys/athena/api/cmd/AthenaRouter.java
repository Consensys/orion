package net.consensys.athena.api.cmd;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.storage.KeyValueStore;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageIdBuilder;
import net.consensys.athena.impl.enclave.BouncyCastleEnclave;
import net.consensys.athena.impl.http.controllers.DeleteController;
import net.consensys.athena.impl.http.controllers.PartyInfoController;
import net.consensys.athena.impl.http.controllers.PushController;
import net.consensys.athena.impl.http.controllers.ReceiveController;
import net.consensys.athena.impl.http.controllers.ResendController;
import net.consensys.athena.impl.http.controllers.SendController;
import net.consensys.athena.impl.http.controllers.UpcheckController;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Router;
import net.consensys.athena.impl.http.server.Serializer;
import net.consensys.athena.impl.storage.Sha512_256StorageIdBuilder;
import net.consensys.athena.impl.storage.StorageKeyValueStorageDelegate;
import net.consensys.athena.impl.storage.file.MapDbStorage;

import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import io.netty.handler.codec.http.HttpRequest;

public class AthenaRouter implements Router {

  private static final Enclave ENCLAVE = new BouncyCastleEnclave();
  private static final StorageIdBuilder KEY_BUILDER = new Sha512_256StorageIdBuilder(ENCLAVE);
  private static final KeyValueStore KEY_VALUE_STORE = new MapDbStorage("routerdb");
  private static final Storage STORAGE =
      new StorageKeyValueStorageDelegate(KEY_VALUE_STORE, KEY_BUILDER);
  private static final Serializer SERIALIZER =
      new Serializer(new ObjectMapper(), new ObjectMapper(new CBORFactory()));

  @Override
  public Controller lookup(HttpRequest request) {
    try {
      URI uri = new URI(request.uri());
      if (uri.getPath().startsWith("/upcheck")) {
        return new UpcheckController();
      }
      if (uri.getPath().startsWith("/sendraw")) {
        return new SendController(ENCLAVE, STORAGE, ContentType.RAW);
      }
      if (uri.getPath().startsWith("/receiveraw")) {
        return new ReceiveController(ENCLAVE, STORAGE, ContentType.RAW, SERIALIZER);
      }
      if (uri.getPath().startsWith("/send")) {
        return new SendController(ENCLAVE, STORAGE, ContentType.JSON);
      }
      if (uri.getPath().startsWith("/receive")) {
        return new ReceiveController(ENCLAVE, STORAGE, ContentType.JSON, SERIALIZER);
      }
      if (uri.getPath().startsWith("/delete")) {
        return new DeleteController(STORAGE);
      }
      if (uri.getPath().startsWith("/resend")) {
        return new ResendController(ENCLAVE, STORAGE);
      }
      if (uri.getPath().startsWith("/partyinfo")) {
        //probably need to inject something that stores the party info state.
        return new PartyInfoController();
      }
      if (uri.getPath().startsWith("/push")) {
        return new PushController(STORAGE, SERIALIZER);
      }

      throw new RuntimeException("Unsupported uri: " + uri);
    } catch (URISyntaxException e) {
      throw new RuntimeException("Unable to handle request.", e);
    }
  }
}
