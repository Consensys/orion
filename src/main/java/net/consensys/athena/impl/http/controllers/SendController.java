package net.consensys.athena.impl.http.controllers;

import static net.consensys.athena.impl.http.data.Result.badRequest;
import static net.consensys.athena.impl.http.data.Result.ok;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Request;
import net.consensys.athena.impl.http.data.Result;
import net.consensys.athena.impl.http.data.Serializer;
import net.consensys.athena.impl.http.server.Controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

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
    SendRequest sendRequest = request.getPayload();
    if (!sendRequest.isValid()) {
      return badRequest("payload, from or to field not properly set");
    }

    log.trace("reading public keys from SendRequest object");
    // read provided public keys
    PublicKey fromKey = enclave.readKey(sendRequest.from);
    Stream<PublicKey> toKeys = Arrays.stream(sendRequest.to).map(b64key -> enclave.readKey(b64key));

    // recipients = toKeys + [nodeAlwaysSendTo] --> default pub key to always send to
    PublicKey[] recipients =
        Stream.concat(Arrays.stream(enclave.alwaysSendTo()), toKeys).toArray(PublicKey[]::new);

    // convert payload from b64 to bytes
    byte[] rawPayload;
    try {
      rawPayload = Base64.getDecoder().decode(sendRequest.payload.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }

    // encrypting payload
    log.trace("encrypting payload from SendRequest object");
    EncryptedPayload encryptedPayload = enclave.encrypt(rawPayload, fromKey, recipients);

    // storing payload
    log.trace("storing payload");
    String digest = storage.put(encryptedPayload);

    // if [to] is not only self, propagate payload to recipients
    // for each t in [to], find the matching IP from public key, and call the /push API with the encryptedPayload
    try {
      for (int i = 0; i < recipients.length; i++) {
        if (nodeKeys.contains(recipients[i])) {
          // do not send payload to self
          continue;
        }

        URL recipientURL = networkNodes.urlForRecipient(recipients[i]);
        URL pushURL = new URL(recipientURL, "/push"); // TODO @gbotrel reverse routing would be nice

        // serialize payload and build RequestBody. we also strip non relevant combinedKeys
        byte[] payload =
            serializer.serialize(encryptedPayload.stripFor(recipients[i]), ContentType.CBOR);
        RequestBody body = RequestBody.create(CBOR, payload);

        // build the request
        okhttp3.Request req = new okhttp3.Request.Builder().url(pushURL).post(body).build();

        // send the request
        Response response =
            httpClient
                .newCall(req)
                .execute(); // TODO @gbotrel perform these requests async with callback
        if ((response.code() != 200) || !(response.body().string().equals(digest))) {
          // error and stop
          throw new IOException("payload propagation failed");
        }
      }
    } catch (IOException io) {
      log.error(io.getMessage());
      //          storage.remove(digest);
      throw new RuntimeException(io);
    }
    return ok(ContentType.JSON, new SendResponse(digest));
  }

  static class SendRequest {
    String payload; // b64 encoded
    String from; // b64 encoded
    String[] to; // b64 encoded

    public boolean isValid() {
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
