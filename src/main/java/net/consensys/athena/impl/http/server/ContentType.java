package net.consensys.athena.impl.http.server;

import java.util.NoSuchElementException;

import io.netty.handler.codec.http.HttpHeaderValues;

public enum ContentType {
  JSON,
  RAW,
  HASKELL_ENCODED,
  JAVA_ENCODED,
  CBOR_ENCODED;

  public static ContentType fromHttpContentEncoding(String contentEncoding) {
    if (contentEncoding.equals(HttpHeaderValues.APPLICATION_JSON.toString())) {
      return JSON;
    } else if (contentEncoding.equals(HttpHeaderValues.APPLICATION_JSON.toString())) {
      return RAW;
    } else {
      throw new NoSuchElementException();
    }
  }
}
