package net.consensys.athena.impl.http.server;

import java.util.NoSuchElementException;

import io.netty.handler.codec.http.HttpHeaderValues;

public enum HttpContentType {
  JSON(HttpHeaderValues.APPLICATION_JSON.toString()),
  BINARY(HttpHeaderValues.BINARY.toString()),
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
}
