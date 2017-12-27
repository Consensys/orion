package net.consensys.athena.impl.http.controllers;

import static net.consensys.athena.impl.http.data.Result.internalServerError;
import static net.consensys.athena.impl.http.data.Result.ok;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.http.data.Base64;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Request;
import net.consensys.athena.impl.http.data.Result;
import net.consensys.athena.impl.http.data.Serializer;
import net.consensys.athena.impl.http.server.Controller;

import java.io.IOException;
import java.net.URL;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Send a base64 encoded payload to encrypt. */
public class SendController implements Controller {
  private static final Logger log = LogManager.getLogger();

  private final Enclave enclave;
  private final Storage storage;
  private final ContentType contentType;
  private final List<PublicKey> nodeKeys;
  private final NetworkNodes networkNodes;
  private final Serializer serializer;

  private final OkHttpClient httpClient = new OkHttpClient();
  private final MediaType CBOR = MediaType.parse(ContentType.CBOR.httpHeaderValue);

  public SendController(
      Enclave enclave,
      Storage storage,
      ContentType contentType,
      NetworkNodes networkNodes,
      Serializer serializer) {
    this.enclave = enclave;
    this.storage = storage;
    this.contentType = contentType;
    this.nodeKeys = Arrays.asList(enclave.nodeKeys());
    this.networkNodes = networkNodes;
    this.serializer = serializer;
  }

  @Override
  public Result handle(Request request) {
    Optional<SendRequest> requestPayload = request.getPayload();
    SendRequest sendRequest = requestPayload.orElseThrow(() -> new IllegalArgumentException());

    if (!sendRequest.isValid()) {
      throw new IllegalArgumentException();
    }

    log.trace("reading public keys from SendRequest object");
    // read provided public keys
    PublicKey fromKey = enclave.readKey(sendRequest.from);
    List<PublicKey> toKeys =
        Arrays.stream(sendRequest.to).map(enclave::readKey).collect(Collectors.toList());

    // toKeys = toKeys + [nodeAlwaysSendTo] --> default pub key to always send to
    toKeys.addAll(Arrays.asList(enclave.alwaysSendTo()));
    PublicKey[] arrToKeys = new PublicKey[toKeys.size()];
    arrToKeys = toKeys.toArray(arrToKeys);

    // convert payload from b64 to bytes
    byte[] rawPayload = Base64.decode(sendRequest.payload);

    // encrypting payload
    log.trace("encrypting payload from SendRequest object");
    EncryptedPayload encryptedPayload = enclave.encrypt(rawPayload, fromKey, arrToKeys);

    // storing payload
    log.trace("storing payload");
    String digest = storage.put(encryptedPayload);

    // propagate payload
    log.trace("propagating payload");
    boolean propagated =
        toKeys
            .stream()
            .parallel()
            .filter(pKey -> !nodeKeys.contains(pKey))
            .map(pKey -> pushToPeer(encryptedPayload, pKey))
            .allMatch(resp -> isValidResponse(resp, digest));

    if (!propagated) {
      log.warn("propagating the payload failed, removing stored encrypted payload");
      storage.remove(digest);
      return internalServerError("couldn't propagate payload to all recipients");
    }

    return ok(ContentType.JSON, new SendResponse(digest));
  }

  private Response pushToPeer(EncryptedPayload encryptedPayload, PublicKey recipient) {
    try {
      URL recipientURL = networkNodes.urlForRecipient(recipient);
      if (recipientURL == null) {
        throw new RuntimeException("couldn't find peer URL");
      }
      URL pushURL = new URL(recipientURL, "/push"); // TODO @gbotrel reverse routing would be nice

      // serialize payload and build RequestBody. we also strip non relevant combinedKeys
      byte[] payload = serializer.serialize(ContentType.CBOR, encryptedPayload.stripFor(recipient));
      RequestBody body = RequestBody.create(CBOR, payload);

      // build request
      okhttp3.Request req = new okhttp3.Request.Builder().url(pushURL).post(body).build();

      // execute request
      return httpClient.newCall(req).execute();
    } catch (IOException io) {
      throw new RuntimeException(io);
      // TODO @gbotrel / reviewer --> in case of network exception, shall we throw RuntimeException
      // or return the following response ?
      //      return new Response.Builder().code(418).build();
    }
  }

  private boolean isValidResponse(Response response, String digest) {
    try {
      return response.code() == 200 && response.body().string().equals(digest);
    } catch (IOException io) {
      throw new RuntimeException(io);
    }
  }

  static class SendRequest {
    String payload; // b64 encoded
    String from; // b64 encoded
    String[] to; // b64 encoded

    @JsonCreator
    public SendRequest(
        @JsonProperty("payload") String payload,
        @JsonProperty("from") String from,
        @JsonProperty("to") String[] to) {
      this.payload = payload;
      this.from = from;
      this.to = to;
    }

    public boolean isValid() {
      if (Stream.of(payload, from, to).anyMatch(Objects::isNull)) {
        return false;
      }
      for (int i = 0; i < to.length; i++) {
        if (to[i].length() <= 0) {
          return false;
        }
      }
      return payload.length() > 0 && from.length() > 0 && to.length > 0;
    }
  }

  static class SendResponse {
    String key; // b64 digest key result from encrypted payload storage operation

    public SendResponse(String key) {
      this.key = key;
    }
  }
}
