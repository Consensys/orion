package net.consensys.athena.impl.http.controllers;

import static net.consensys.athena.impl.http.data.Result.badRequest;
import static net.consensys.athena.impl.http.data.Result.notImplemented;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Request;
import net.consensys.athena.impl.http.data.Result;
import net.consensys.athena.impl.http.server.Controller;

/** Send a base64 encoded payload to encrypt. */
public class SendController implements Controller {
  private final Enclave enclave;
  private final Storage storage;
  private final ContentType contentType;

  public SendController(Enclave enclave, Storage storage, ContentType contentType) {
    this.enclave = enclave;
    this.storage = storage;
    this.contentType = contentType;
  }

  @Override
  public Result handle(Request request) {
    SendRequest sendRequest = request.getPayload();
    if (!sendRequest.isValid()) {
      return badRequest("payload, from or to field not properly set");
    }

    // if request.from == null, use default node public key as "from"
    // to = to + [nodeAlwaysSendTo] --> default pub key to always send to
    // if to == null, set to to self public key
    // TODO : that's a port from original Haskell code --> shouldn't we just validate the send request ? if to is set, we sent to "to", if not, we  return error ?
    // convert payload from b64 to bytes
    // encryptedPayload = enclave.encrypt(sendRequest.payload, from, to);
    // toReturn = storage.store(encryptedPayload);
    // if [to] is not only self, propagate payload to receipients
    // for each t in [to], find the matching IP from public key, and call the /push API with the encryptedPayload
    return notImplemented();
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
  }
}
