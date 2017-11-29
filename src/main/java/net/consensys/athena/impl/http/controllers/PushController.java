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

/** used to push a payload to a node. */
public class PushController implements Controller {
  private final Storage storage;

  public PushController(Storage storage) {
    this.storage = storage;
  }

  @Override
  public FullHttpResponse handle(FullHttpRequest request, FullHttpResponse response) {
    // we receive a encrypted payload (binary content) and store it into storage system
    StorageData toStore = new SimpleStorage(request.content().array());
    StorageKey digest = storage.store(toStore);

    // return the digest (key)
    ByteBuf content =
        Unpooled.copiedBuffer(digest.getBase64Encoded().getBytes(Charset.forName("utf8")));
    return response.replace(content);
  }
}
