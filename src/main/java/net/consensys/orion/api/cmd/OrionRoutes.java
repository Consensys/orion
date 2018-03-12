package net.consensys.orion.api.cmd;

import static net.consensys.orion.impl.http.server.HttpContentType.*;

import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.network.NetworkNodes;
import net.consensys.orion.api.storage.Storage;
import net.consensys.orion.api.storage.StorageEngine;
import net.consensys.orion.api.storage.StorageKeyBuilder;
import net.consensys.orion.impl.http.handler.partyinfo.PartyInfoHandler;
import net.consensys.orion.impl.http.handler.push.PushHandler;
import net.consensys.orion.impl.http.handler.receive.ReceiveHandler;
import net.consensys.orion.impl.http.handler.send.SendHandler;
import net.consensys.orion.impl.http.handler.upcheck.UpcheckHandler;
import net.consensys.orion.impl.http.server.vertx.HttpErrorHandler;
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

  private final Router router;

  public OrionRoutes(
      Vertx vertx,
      NetworkNodes networkNodes,
      Serializer serializer,
      Enclave enclave,
      StorageEngine<EncryptedPayload> storageEngine) {
    // controller dependencies
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
        .handler(new SendHandler(enclave, storage, networkNodes, serializer, JSON));
    router
        .post(SEND_RAW)
        .produces(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .consumes(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .handler(
            new SendHandler(enclave, storage, networkNodes, serializer, APPLICATION_OCTET_STREAM));

    router
        .post(RECEIVE)
        .produces(JSON.httpHeaderValue)
        .consumes(JSON.httpHeaderValue)
        .handler(new ReceiveHandler(enclave, storage, serializer, JSON));
    router
        .post(RECEIVE_RAW)
        .produces(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .consumes(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .handler(new ReceiveHandler(enclave, storage, serializer, APPLICATION_OCTET_STREAM));

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
