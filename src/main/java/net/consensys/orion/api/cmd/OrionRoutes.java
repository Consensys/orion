package net.consensys.orion.api.cmd;

import static net.consensys.orion.impl.http.server.HttpContentType.APPLICATION_OCTET_STREAM;
import static net.consensys.orion.impl.http.server.HttpContentType.CBOR;
import static net.consensys.orion.impl.http.server.HttpContentType.JSON;
import static net.consensys.orion.impl.http.server.HttpContentType.TEXT;

import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.storage.Storage;
import net.consensys.orion.api.storage.StorageEngine;
import net.consensys.orion.api.storage.StorageKeyBuilder;
import net.consensys.orion.impl.http.handler.partyinfo.PartyInfoHandler;
import net.consensys.orion.impl.http.handler.push.PushHandler;
import net.consensys.orion.impl.http.handler.receive.ReceiveHandler;
import net.consensys.orion.impl.http.handler.send.SendHandler;
import net.consensys.orion.impl.http.handler.upcheck.UpcheckHandler;
import net.consensys.orion.impl.http.server.vertx.HttpErrorHandler;
import net.consensys.orion.impl.network.ConcurrentNetworkNodes;
import net.consensys.orion.impl.storage.EncryptedPayloadStorage;
import net.consensys.orion.impl.storage.Sha512_256StorageKeyBuilder;
import net.consensys.orion.impl.utils.Serializer;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;

public class OrionRoutes {

  // route paths
  public static final String UPCHECK = "/upcheck";
  public static final String SEND = "/send";
  public static final String RECEIVE = "/receive";
  public static final String SEND_RAW = "/sendraw";
  public static final String RECEIVE_RAW = "/receiveraw";
  public static final String PARTYINFO = "/partyinfo";
  public static final String DELETE = "/delete";
  public static final String PUSH = "/push";

  private final Storage storage;

  private final Router publicRouter;

  private final Router privateRouter;

  public OrionRoutes(
      Vertx vertx,
      ConcurrentNetworkNodes networkNodes,
      Serializer serializer,
      Enclave enclave,
      StorageEngine<EncryptedPayload> storageEngine) {
    // controller dependencies
    StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder(enclave);

    this.storage = new EncryptedPayloadStorage(storageEngine, keyBuilder);

    // Vertx routers
    publicRouter = Router.router(vertx);
    privateRouter = Router.router(vertx);

    // sets response content-type from Accept header
    // and handle errors

    LoggerHandler loggerHandler = LoggerHandler.create();

    //Setup Pulblic APIs
    publicRouter
        .route()
        .handler(BodyHandler.create())
        .handler(loggerHandler)
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(new HttpErrorHandler(serializer));

    publicRouter.get(UPCHECK).produces(TEXT.httpHeaderValue).handler(new UpcheckHandler());

    publicRouter
        .post(PARTYINFO)
        .produces(CBOR.httpHeaderValue)
        .consumes(CBOR.httpHeaderValue)
        .handler(new PartyInfoHandler(networkNodes, serializer));

    publicRouter
        .post(PUSH)
        .produces(TEXT.httpHeaderValue)
        .consumes(CBOR.httpHeaderValue)
        .handler(new PushHandler(storage, serializer));

    //Setup Private APIs
    privateRouter
        .route()
        .handler(BodyHandler.create())
        .handler(loggerHandler)
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(new HttpErrorHandler(serializer));

    privateRouter.get(UPCHECK).produces(TEXT.httpHeaderValue).handler(new UpcheckHandler());

    privateRouter
        .post(SEND)
        .produces(JSON.httpHeaderValue)
        .consumes(JSON.httpHeaderValue)
        .handler(new SendHandler(vertx, enclave, storage, networkNodes, serializer, JSON));
    privateRouter
        .post(SEND_RAW)
        .produces(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .consumes(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .handler(
            new SendHandler(
                vertx, enclave, storage, networkNodes, serializer, APPLICATION_OCTET_STREAM));

    privateRouter
        .post(RECEIVE)
        .produces(JSON.httpHeaderValue)
        .consumes(JSON.httpHeaderValue)
        .handler(new ReceiveHandler(enclave, storage, serializer, JSON));
    privateRouter
        .post(RECEIVE_RAW)
        .produces(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .consumes(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .handler(new ReceiveHandler(enclave, storage, serializer, APPLICATION_OCTET_STREAM));
  }

  public Storage getStorage() {
    return storage;
  }

  public Router publicRouter() {
    return publicRouter;
  }

  public Router privateRouter() {
    return privateRouter;
  }
}
