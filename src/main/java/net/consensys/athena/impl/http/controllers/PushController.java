package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageKey;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.storage.SimpleStorage;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;

/** used to push a payload to a node. */
public class PushController implements Controller {
  private final Storage storage;

  public PushController(Storage storage) {
    this.storage = storage;
  }

  @Override
  public FullHttpResponse handle(FullHttpRequest request, FullHttpResponse response) {
    // ensure HTTP method = POST
    if (request.method() != HttpMethod.POST) {
      return response.setStatus(HttpResponseStatus.BAD_REQUEST);
    }

    // read the requestPayload, "Netty way"
    byte[] requestPayload;
    int length = request.content().readableBytes();
    if (length <= 0) { // empty payload
      return response.setStatus(HttpResponseStatus.BAD_REQUEST);
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
    ByteBuf content =
        Unpooled.copiedBuffer(digest.getBase64Encoded().getBytes(Charset.forName("utf8")));
    return response.replace(content);
  }
}
