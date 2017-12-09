package net.consensys.athena.impl.http.controllers;

import static net.consensys.athena.impl.http.server.Result.*;

import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageKey;
import net.consensys.athena.impl.http.server.ContentType;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Result;
import net.consensys.athena.impl.storage.SimpleStorage;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;

/** used to push a payload to a node. */
public class PushController implements Controller {
  private final Storage storage;

  public PushController(Storage storage) {
    this.storage = storage;
  }

  @Override
  public Result handle(FullHttpRequest request) {
    // ensure HTTP method = POST
    if (request.method() != HttpMethod.POST) {
      return badRequest(ContentType.JSON);
    }

    // read the requestPayload, "Netty way"
    byte[] requestPayload;
    int length = request.content().readableBytes();
    if (length <= 0) { // empty payload
      return badRequest(ContentType.JSON);
    }

    if (request.content().hasArray()) {
      requestPayload = request.content().array();
    } else {
      requestPayload = new byte[length];
      request.content().getBytes(request.content().readerIndex(), requestPayload);
    }

    // we receive a encrypted payload (binary content) and store it into storage system
    StorageData toStore = new SimpleStorage(requestPayload);
    StorageKey digest = storage.store(toStore);

    // return the digest (key)
    //    ByteBuf content =
    //        Unpooled.copiedBuffer(digest.getBase64Encoded().getBytes(Charset.forName("utf8")));
    return ok(ContentType.JSON, digest.getBase64Encoded());
  }
}
