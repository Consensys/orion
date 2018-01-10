package net.consensys.athena.api.cmd;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageEngine;
import net.consensys.athena.api.storage.StorageKeyBuilder;
import net.consensys.athena.impl.http.handler.delete.DeleteHandler;
import net.consensys.athena.impl.http.handler.partyinfo.PartyInfoHandler;
import net.consensys.athena.impl.http.handler.push.PushHandler;
import net.consensys.athena.impl.http.handler.receive.ReceiveHandler;
import net.consensys.athena.impl.http.handler.send.SendHandler;
import net.consensys.athena.impl.http.handler.upcheck.UpcheckHandler;
import net.consensys.athena.impl.http.server.HttpContentType;
import net.consensys.athena.impl.http.server.vertx.HttpErrorHandler;
import net.consensys.athena.impl.storage.EncryptedPayloadStorage;
import net.consensys.athena.impl.storage.Sha512_256StorageKeyBuilder;
import net.consensys.athena.impl.storage.file.MapDbStorage;
import net.consensys.athena.impl.utils.Serializer;

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
        .failureHandler(new HttpErrorHandler(serializer));

    router
        .get(UPCHECK)
        .produces(HttpContentType.TEXT.httpHeaderValue)
        .handler(new UpcheckHandler());

    router
        .post(SEND)
        .produces(HttpContentType.JSON.httpHeaderValue)
        .handler(new SendHandler(enclave, storage, networkNodes, serializer));

    router
        .post(RECIEVE)
        .produces(HttpContentType.JSON.httpHeaderValue)
        .handler(BodyHandler.create())
        .handler(new ReceiveHandler(enclave, storage, serializer));

    router.post(DELETE).handler(BodyHandler.create()).handler(new DeleteHandler(storage));

    router
        .get(PARTYINFO)
        .produces(HttpContentType.JSON.httpHeaderValue)
        .handler(BodyHandler.create())
        .handler(new PartyInfoHandler(networkNodes, serializer));

    router
        .post(PUSH)
        .produces(HttpContentType.TEXT.httpHeaderValue)
        .handler(BodyHandler.create())
        .handler(new PushHandler(storage, serializer));
  }

  public Storage getStorage() {
    return storage;
  }

  public Router getRouter() {
    return router;
  }
}
