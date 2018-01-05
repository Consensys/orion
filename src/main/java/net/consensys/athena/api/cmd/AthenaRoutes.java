package net.consensys.athena.api.cmd;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageEngine;
import net.consensys.athena.api.storage.StorageKeyBuilder;
import net.consensys.athena.impl.enclave.sodium.LibSodiumEnclave;
import net.consensys.athena.impl.enclave.sodium.SodiumFileKeyStore;
import net.consensys.athena.impl.http.controllers.ApiErrorHandler;
import net.consensys.athena.impl.http.controllers.DeleteController;
import net.consensys.athena.impl.http.controllers.PartyInfoController;
import net.consensys.athena.impl.http.controllers.PushController;
import net.consensys.athena.impl.http.controllers.ReceiveController;
import net.consensys.athena.impl.http.controllers.SendController;
import net.consensys.athena.impl.http.controllers.UpcheckController;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Serializer;
import net.consensys.athena.impl.storage.EncryptedPayloadStorage;
import net.consensys.athena.impl.storage.Sha512_256StorageKeyBuilder;
import net.consensys.athena.impl.storage.file.MapDbStorage;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;

public class AthenaRoutes {

  private static final StorageEngine<EncryptedPayload> STORAGE_ENGINE =
      new MapDbStorage("routerdb");

  private final Enclave enclave;
  private final Serializer serializer;
  private final Storage storage;
  private final NetworkNodes networkNodes;

  private final Router router;

  public AthenaRoutes(
      Vertx vertx, NetworkNodes networkNodes, Config config, Serializer serializer) {
    // controller dependencies
    this.enclave = new LibSodiumEnclave(config, new SodiumFileKeyStore(config, serializer));
    StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder(enclave);
    this.storage = new EncryptedPayloadStorage(STORAGE_ENGINE, keyBuilder);
    this.serializer = serializer;
    this.networkNodes = networkNodes;

    // Vertx router
    router = Router.router(vertx);

    // sets response content-type from Accept header
    router.route().handler(ResponseContentTypeHandler.create());
    // catch errors from controllers to return standard ApiError
    router.route().failureHandler(new ApiErrorHandler(serializer));

    router
        .get("/upcheck")
        .produces(ContentType.TEXT.httpHeaderValue)
        .handler(new UpcheckController());

    router
        .post("/send")
        .produces(ContentType.JSON.httpHeaderValue)
        .handler(BodyHandler.create())
        .handler(new SendController(enclave, storage, networkNodes, serializer));

    router
        .post("/receive")
        .produces(ContentType.JSON.httpHeaderValue)
        .handler(BodyHandler.create())
        .handler(new ReceiveController(enclave, storage, serializer));

    router.post("/delete").handler(BodyHandler.create()).handler(new DeleteController(storage));

    router
        .get("/partyinfo")
        .produces(ContentType.JSON.httpHeaderValue)
        .handler(BodyHandler.create())
        .handler(new PartyInfoController(networkNodes, serializer));

    router
        .post("/push")
        .produces(ContentType.TEXT.httpHeaderValue)
        .handler(BodyHandler.create())
        .handler(new PushController(storage, serializer));
  }

  public Router getRouter() {
    return router;
  }
}
