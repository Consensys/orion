package net.consensys.athena.impl.http.data;

import java.util.NoSuchElementException;

import io.netty.handler.codec.http.HttpHeaderValues;

public enum ContentType {
  JSON(HttpHeaderValues.APPLICATION_JSON.toString()),
  BINARY(HttpHeaderValues.BINARY.toString()),
  TEXT(HttpHeaderValues.TEXT_PLAIN.toString() + "; charset=utf-8"),
  HASKELL_ENCODED("application/haskell-stream"),
  JAVA_ENCODED("application/java-stream"),
  CBOR("application/cbor");

  public final String httpHeaderValue;

  ContentType(String httpHeaderValue) {
    this.httpHeaderValue = httpHeaderValue;
  }

  public static ContentType fromHttpContentEncoding(String contentEncoding) {
    for (ContentType cType : ContentType.values()) {
      if (cType.httpHeaderValue.equalsIgnoreCase(contentEncoding)) {
        return cType;
      }
    }
    throw new NoSuchElementException();
  }
}
