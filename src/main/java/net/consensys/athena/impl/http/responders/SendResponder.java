package net.consensys.athena.impl.http.responders;

import net.consensys.athena.impl.http.server.AbstractResponder;
import net.consensys.athena.impl.http.server.ContentType;
import net.consensys.athena.impl.http.server.Responder;

import io.netty.handler.codec.http.FullHttpResponse;

/**
 * Responder for send, supports a default content type so the raw and standard controllers will both
 * reuse this controller.
 */
public class SendResponder extends AbstractResponder implements Responder {

  public SendResponder(FullHttpResponse response, ContentType defaultContentType) {
    super(response, defaultContentType);
  }

  @Override
  public byte[] getRaw() {
    return "abcd".getBytes();
  }

  @Override
  public String getJson() {
    return "{\"key\":\"abcd\"}";
  }

  @Override
  public byte[] getHaskellEncoded() {
    return new byte[0];
  }
}
