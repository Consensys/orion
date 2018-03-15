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

import java.io.IOException;
import java.net.URL;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
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

  private final OkHttpClient httpClient = new OkHttpClient();
  private final MediaType CBOR = MediaType.parse(HttpContentType.CBOR.httpHeaderValue);

  public SendHandler(
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
    log.debug(sendRequest.rawPayload());

    if (!sendRequest.isValid()) {
      throw new IllegalArgumentException();
    }

    log.debug("reading public keys from SendRequest object");
    // read provided public keys
    final PublicKey fromKey = enclave.readKey(sendRequest.from());
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

    // storing payload
    log.debug("storing payload");
    final String digest = storage.put(encryptedPayload);

    // propagate payload
    log.debug("propagating payload");
    final boolean propagated =
        toKeys
            .stream()
            .parallel()
            .filter(pKey -> !nodeKeys.contains(pKey))
            .map(pKey -> pushToPeer(encryptedPayload, pKey))
            .allMatch(resp -> isValidResponse(resp, digest));

    if (!propagated) {
      log.warn("propagating the payload failed, removing stored encrypted payload");
      storage.remove(digest);
      routingContext.fail(
          new OrionException(
              OrionErrorCode.NODE_PROPAGATION_TO_ALL_PEERS,
              "couldn't propagate payload to all recipients"));
      return;
    }

    final Buffer responseData;
    if (contentType == JSON) {
      responseData = Buffer.buffer(serializer.serialize(JSON, new SendResponse(digest)));
    } else {
      responseData = Buffer.buffer(digest);
    }

    routingContext.response().end(responseData);
  }

  private SendRequest binaryRequest(RoutingContext routingContext) {
    String from = routingContext.request().getHeader("c11n-from");
    String[] to = routingContext.request().getHeader("c11n-to").split(",");
    return new SendRequest(routingContext.getBody().getBytes(), from, to);
  }

  private Response pushToPeer(EncryptedPayload encryptedPayload, PublicKey recipient) {
    try {
      final URL recipientURL = networkNodes.urlForRecipient(recipient);
      if (recipientURL == null) {
        throw new OrionException(OrionErrorCode.NODE_MISSING_PEER_URL, "couldn't find peer URL");
      }
      final URL pushURL = new URL(recipientURL, OrionRoutes.PUSH);

      // serialize payload and build RequestBody. we also strip non relevant combinedKeys
      final byte[] payload =
          serializer.serialize(HttpContentType.CBOR, encryptedPayload.stripFor(recipient));
      final RequestBody body = RequestBody.create(CBOR, payload);

      // build request
      okhttp3.Request req = new okhttp3.Request.Builder().url(pushURL).post(body).build();

      // execute request
      return httpClient.newCall(req).execute();
    } catch (IOException io) {
      throw new OrionException(OrionErrorCode.NODE_PUSHING_TO_PEER, io);
    }
  }

  private boolean isValidResponse(Response response, String digest) {
    try {
      return response.code() == 200 && response.body().string().equals(digest);
    } catch (IOException io) {
      throw new OrionException(OrionErrorCode.NODE_PUSHING_TO_PEER_RESPONSE, io);
    }
  }
}
