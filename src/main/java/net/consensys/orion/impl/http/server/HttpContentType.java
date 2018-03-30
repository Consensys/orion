package net.consensys.orion.impl.http.server;


import io.netty.handler.codec.http.HttpHeaderValues;

public enum HttpContentType {
  JSON(HttpHeaderValues.APPLICATION_JSON.toString()),
  APPLICATION_OCTET_STREAM(HttpHeaderValues.APPLICATION_OCTET_STREAM.toString()),
  TEXT(HttpHeaderValues.TEXT_PLAIN.toString() + "; charset=utf-8"),
  CBOR("application/cbor");

  public final String httpHeaderValue;

  HttpContentType(String httpHeaderValue) {
    this.httpHeaderValue = httpHeaderValue;
  }

  @Override
  public String toString() {
    return httpHeaderValue;
  }
}
