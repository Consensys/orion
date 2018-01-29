package net.consensys.athena.api.cmd;

import static net.consensys.athena.impl.http.server.HttpContentType.*;

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
import net.consensys.athena.impl.http.server.vertx.HttpErrorHandler;
import net.consensys.athena.impl.storage.EncryptedPayloadStorage;
import net.consensys.athena.impl.storage.Sha512_256StorageKeyBuilder;
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

  private final Enclave enclave;
  private final Storage storage;

  private final Router router;

  public AthenaRoutes(
      Vertx vertx,
      NetworkNodes networkNodes,
      Serializer serializer,
      Enclave enclave,
      StorageEngine<EncryptedPayload> storageEngine) {
    // controller dependencies
    this.enclave = enclave;
    StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder(enclave);

    this.storage = new EncryptedPayloadStorage(storageEngine, keyBuilder);

    // Vertx router
    router = Router.router(vertx);

    // sets response content-type from Accept header
    // and handle errors

    LoggerHandler loggerHandler = LoggerHandler.create();

    router
        .route()
        .handler(BodyHandler.create())
        .handler(loggerHandler)
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(new HttpErrorHandler(serializer));

    router.get(UPCHECK).produces(TEXT.httpHeaderValue).handler(new UpcheckHandler());

    router
        .post(SEND)
        .produces(JSON.httpHeaderValue)
        .consumes(JSON.httpHeaderValue)
        .handler(new SendHandler(enclave, storage, networkNodes, serializer));

    router
        .post(RECIEVE)
        .produces(JSON.httpHeaderValue)
        .consumes(JSON.httpHeaderValue)
        .handler(new ReceiveHandler(enclave, storage, serializer));

    router.post(DELETE).handler(new DeleteHandler(storage));

    router
        .post(PARTYINFO)
        .produces(CBOR.httpHeaderValue)
        .consumes(CBOR.httpHeaderValue)
        .handler(new PartyInfoHandler(networkNodes, serializer));

    router
        .post(PUSH)
        .produces(TEXT.httpHeaderValue)
        .consumes(CBOR.httpHeaderValue)
        .handler(new PushHandler(storage, serializer));
  }

  public Storage getStorage() {
    return storage;
  }

  public Router getRouter() {
    return router;
  }
}
