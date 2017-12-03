package net.consensys.athena.impl.http.responders;

import net.consensys.athena.impl.http.server.AbstractResponder;
import net.consensys.athena.impl.http.server.ContentType;
import net.consensys.athena.impl.http.server.Responder;

import com.google.common.base.Charsets;
import io.netty.handler.codec.http.FullHttpResponse;

public class UpcheckResponder extends AbstractResponder implements Responder {

  public UpcheckResponder(FullHttpResponse response) {
    super(response, ContentType.RAW);
  }

  @Override
  public byte[] getRaw() {
    return "I'm up!\n".getBytes(Charsets.UTF_8);
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
