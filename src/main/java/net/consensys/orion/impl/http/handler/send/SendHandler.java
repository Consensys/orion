package net.consensys.orion.impl.http.handler.send;

import static net.consensys.orion.impl.http.server.HttpContentType.JSON;

import net.consensys.orion.api.cmd.OrionRoutes;
import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.api.exception.OrionException;
import net.consensys.orion.api.storage.Storage;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.network.ConcurrentNetworkNodes;
import net.consensys.orion.impl.utils.Serializer;

import java.net.URL;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Send a base64 encoded payload to encrypt. */
public class SendHandler implements Handler<RoutingContext> {
  private static final Logger log = LogManager.getLogger();

  private final Enclave enclave;
  private final Storage storage;
  private final List<PublicKey> nodeKeys;
  private final ConcurrentNetworkNodes networkNodes;
  private final Serializer serializer;
  private final HttpContentType contentType;

  private final HttpClient httpClient;

  public SendHandler(
      Vertx vertx,
      Enclave enclave,
      Storage storage,
      ConcurrentNetworkNodes networkNodes,
      Serializer serializer,
      HttpContentType contentType) {
    this.enclave = enclave;
    this.storage = storage;
    this.nodeKeys = Arrays.asList(enclave.nodeKeys());
    this.networkNodes = networkNodes;
    this.serializer = serializer;
    this.contentType = contentType;
    this.httpClient = vertx.createHttpClient();
  }

  @Override
  public void handle(RoutingContext routingContext) {
    final SendRequest sendRequest;
    if (contentType == JSON) {
      sendRequest =
          serializer.deserialize(JSON, SendRequest.class, routingContext.getBody().getBytes());
    } else {
      sendRequest = binaryRequest(routingContext);
    }
    log.debug(sendRequest);

    if (!sendRequest.isValid()) {
      throw new OrionException(OrionErrorCode.INVALID_PAYLOAD);
    }

    log.debug("reading public keys from SendRequest object");
    // read provided public keys
    PublicKey fromKey =
        sendRequest
            .from()
            .map(enclave::readKey)
            .orElseGet(
                () -> {
                  if (nodeKeys.isEmpty()) {
                    throw new OrionException(OrionErrorCode.NO_SENDER_KEY);
                  }
                  return nodeKeys.get(0);
                });

    final List<PublicKey> toKeys =
        Arrays.stream(sendRequest.to()).map(enclave::readKey).collect(Collectors.toList());

    // toKeys = toKeys + [nodeAlwaysSendTo] --> default pub key to always send to
    toKeys.addAll(Arrays.asList(enclave.alwaysSendTo()));
    PublicKey[] arrToKeys = new PublicKey[toKeys.size()];
    arrToKeys = toKeys.toArray(arrToKeys);

    // convert payload from b64 to bytes
    final byte[] rawPayload = sendRequest.rawPayload();

    // encrypting payload
    log.debug("encrypting payload from SendRequest object");
    final EncryptedPayload encryptedPayload = enclave.encrypt(rawPayload, fromKey, arrToKeys);

    List<PublicKey> keys =
        toKeys.stream().filter(pKey -> !nodeKeys.contains(pKey)).collect(Collectors.toList());

    if (keys.stream().anyMatch(pKey -> networkNodes.urlForRecipient(pKey) == null)) {
      CompletableFuture<Boolean> errorFuture = new CompletableFuture<>();
      routingContext.fail(
          new OrionException(OrionErrorCode.NODE_MISSING_PEER_URL, "couldn't find peer URL"));
      return;
    }

    // storing payload
    log.debug("storing payload");
    final String digest = storage.put(encryptedPayload);

    // propagate payload
    log.debug("propagating payload");
    List<CompletableFuture<Boolean>> futures =
        keys.stream()
            .map(
                pKey -> {
                  URL recipientURL = networkNodes.urlForRecipient(pKey);

                  CompletableFuture<Boolean> responseFuture = new CompletableFuture<>();

                  // serialize payload, stripping non-relevant combinedKeys, and build payload
                  final byte[] payload =
                      serializer.serialize(HttpContentType.CBOR, encryptedPayload.stripFor(pKey));

                  // execute request
                  httpClient
                      .post(recipientURL.getPort(), recipientURL.getHost(), OrionRoutes.PUSH)
                      .putHeader("Content-Type", "application/cbor")
                      .handler(
                          response ->
                              response.bodyHandler(
                                  responseBody -> {
                                    if (response.statusCode() != 200
                                        || !digest.equals(responseBody.toString())) {
                                      responseFuture.completeExceptionally(
                                          new OrionException(
                                              OrionErrorCode.NODE_PROPAGATING_TO_ALL_PEERS));
                                    } else {
                                      responseFuture.complete(true);
                                    }
                                  }))
                      .exceptionHandler(
                          ex ->
                              responseFuture.completeExceptionally(
                                  new OrionException(OrionErrorCode.NODE_PUSHING_TO_PEER, ex)))
                      .end(Buffer.buffer(payload));

                  return responseFuture;
                })
            .collect(Collectors.toList());

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
        .whenComplete(
            (all, ex) -> {
              if (ex != null) {
                log.warn("propagating the payload failed, removing stored encrypted payload");
                storage.remove(digest);

                Throwable cause = ex.getCause();
                if (cause instanceof OrionException) {
                  routingContext.fail(cause);
                } else {
                  routingContext.fail(
                      new OrionException(OrionErrorCode.NODE_PROPAGATING_TO_ALL_PEERS, ex));
                }
                return;
              }

              final Buffer responseData;
              if (contentType == JSON) {
                responseData = Buffer.buffer(serializer.serialize(JSON, new SendResponse(digest)));
              } else {
                responseData = Buffer.buffer(digest);
              }
              routingContext.response().end(responseData);
            });
  }

  private SendRequest binaryRequest(RoutingContext routingContext) {
    String from = routingContext.request().getHeader("c11n-from");
    String[] to = routingContext.request().getHeader("c11n-to").split(",");
    return new SendRequest(routingContext.getBody().getBytes(), from, to);
  }
}
