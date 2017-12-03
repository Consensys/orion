package net.consensys.athena.impl.http.responders;

import net.consensys.athena.impl.http.server.AbstractResponder;
import net.consensys.athena.impl.http.server.Responder;

import io.netty.handler.codec.http.FullHttpResponse;

public class DeleteResponder extends AbstractResponder implements Responder {

  public DeleteResponder(FullHttpResponse response) {
    super(response);
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
