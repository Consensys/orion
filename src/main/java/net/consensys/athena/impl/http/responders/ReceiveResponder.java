package net.consensys.athena.impl.http.responders;

import net.consensys.athena.impl.http.server.AbstractResponder;
import net.consensys.athena.impl.http.server.ContentType;
import net.consensys.athena.impl.http.server.Responder;

import io.netty.handler.codec.http.FullHttpResponse;

public class ReceiveResponder extends AbstractResponder implements Responder {

  public ReceiveResponder(FullHttpResponse response, ContentType defaultContentType) {
    super(response, defaultContentType);
  }

  @Override
  public byte[] getRaw() {
    return new byte[0];
  }

  @Override
  public String getJson() {
    return null;
  }

  @Override
  public byte[] getHaskellEncoded() {
    return new byte[0];
  }
}
