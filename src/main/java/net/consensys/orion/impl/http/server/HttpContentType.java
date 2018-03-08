package net.consensys.orion.impl.http.server;

import java.util.NoSuchElementException;

import io.netty.handler.codec.http.HttpHeaderValues;

public enum HttpContentType {
  JSON(HttpHeaderValues.APPLICATION_JSON.toString()),
  APPLICATION_OCTET_STREAM(HttpHeaderValues.APPLICATION_OCTET_STREAM.toString()),
  TEXT(HttpHeaderValues.TEXT_PLAIN.toString() + "; charset=utf-8"),
  HASKELL_ENCODED("application/haskell-stream"),
  CBOR("application/cbor");

  public final String httpHeaderValue;

  HttpContentType(String httpHeaderValue) {
    this.httpHeaderValue = httpHeaderValue;
  }

  public static HttpContentType fromHttpHeader(String contentType) {
    for (HttpContentType cType : HttpContentType.values()) {
      if (cType.httpHeaderValue.equalsIgnoreCase(contentType)) {
        return cType;
      }
    }
    throw new NoSuchElementException();
  }

  @Override
  public String toString() {
    return httpHeaderValue;
  }
}
