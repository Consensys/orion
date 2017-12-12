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
    } else if (contentEncoding.equals(HttpHeaderValues.APPLICATION_OCTET_STREAM.toString())) {
      return RAW;
    } else {
      throw new NoSuchElementException();
    }
  }

  public String httpHeaderValue() {
    switch (this) {
      case JSON:
        return HttpHeaderValues.APPLICATION_JSON.toString();
      case RAW:
        return HttpHeaderValues.APPLICATION_OCTET_STREAM.toString();
      case JAVA_ENCODED:
        return "application/java-stream";
      case HASKELL_ENCODED:
        return "application/haskell-stream";
      case CBOR_ENCODED:
        return "application/cbor";
    }
  }
}
