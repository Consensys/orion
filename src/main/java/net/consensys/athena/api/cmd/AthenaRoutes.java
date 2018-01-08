package net.consensys.athena.api.cmd;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageEngine;
import net.consensys.athena.api.storage.StorageKeyBuilder;
import net.consensys.athena.impl.http.controllers.DeleteController;
import net.consensys.athena.impl.http.controllers.PartyInfoController;
import net.consensys.athena.impl.http.controllers.PushController;
import net.consensys.athena.impl.http.controllers.ReceiveController;
import net.consensys.athena.impl.http.controllers.SendController;
import net.consensys.athena.impl.http.controllers.SendController.SendRequest;
import net.consensys.athena.impl.http.controllers.UpcheckController;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Serializer;
import net.consensys.athena.impl.http.server.vertx.ApiErrorHandler;
import net.consensys.athena.impl.http.server.vertx.RequestSerializationHandler;
import net.consensys.athena.impl.http.server.vertx.ResponseSerializationHandler;
import net.consensys.athena.impl.storage.EncryptedPayloadStorage;
import net.consensys.athena.impl.storage.Sha512_256StorageKeyBuilder;
import net.consensys.athena.impl.storage.file.MapDbStorage;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;

public class AthenaRoutes {

  // route paths
  public static final String UPCHECK = "/upcheck";
  public static final String SEND = "/send";
  public static final String RECIEVE = "/receive";
  public static final String PARTYINFO = "/partyinfo";
  public static final String DELETE = "/delete";
  public static final String PUSH = "/push";

  private static final StorageEngine<EncryptedPayload> STORAGE_ENGINE =
      new MapDbStorage("routerdb");

  private final Enclave enclave;
  private final Serializer serializer;
  private final Storage storage;
  private final NetworkNodes networkNodes;

  private final Router router;

  public AthenaRoutes(
      Vertx vertx, NetworkNodes networkNodes, Serializer serializer, Enclave enclave) {
    // controller dependencies
    this.enclave = enclave;
    StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder(enclave);
    this.storage = new EncryptedPayloadStorage(STORAGE_ENGINE, keyBuilder);
    this.serializer = serializer;
    this.networkNodes = networkNodes;

    // Vertx router
    router = Router.router(vertx);

    // sets response content-type from Accept header
    // and handle errors
    router
        .route()
        .handler(BodyHandler.create())
        .handler(LoggerHandler.create())
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(new ApiErrorHandler(serializer));

    router.get(UPCHECK).produces(ContentType.TEXT.httpHeaderValue).handler(new UpcheckController());

    router
        .post(SEND)
        .produces(ContentType.JSON.httpHeaderValue)
        .handler(new RequestSerializationHandler(serializer, ContentType.JSON, SendRequest.class))
        .handler(new SendController(enclave, storage, networkNodes, serializer))
        .handler(new ResponseSerializationHandler(serializer, ContentType.JSON));

    router
        .post(RECIEVE)
        .produces(ContentType.JSON.httpHeaderValue)
        .handler(BodyHandler.create())
        .handler(new ReceiveController(enclave, storage, serializer));

    router.post(DELETE).handler(BodyHandler.create()).handler(new DeleteController(storage));

    router
        .get(PARTYINFO)
        .produces(ContentType.JSON.httpHeaderValue)
        .handler(BodyHandler.create())
        .handler(new PartyInfoController(networkNodes, serializer));

    router
        .post(PUSH)
        .produces(ContentType.TEXT.httpHeaderValue)
        .handler(BodyHandler.create())
        .handler(new PushController(storage, serializer));
  }

  public Storage getStorage() {
    return storage;
  }

  public Router getRouter() {
    return router;
  }
}
