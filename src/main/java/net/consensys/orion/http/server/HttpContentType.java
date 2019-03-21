/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.orion.http.server;

import io.netty.handler.codec.http.HttpHeaderValues;

public enum HttpContentType {
  JSON(HttpHeaderValues.APPLICATION_JSON.toString()),
  APPLICATION_OCTET_STREAM(HttpHeaderValues.APPLICATION_OCTET_STREAM.toString()),
  TEXT(HttpHeaderValues.TEXT_PLAIN.toString() + "; charset=utf-8"),
  CBOR("application/cbor"),
  ORION("application/vnd.orion.v1+json");

  public final String httpHeaderValue;

  HttpContentType(String httpHeaderValue) {
    this.httpHeaderValue = httpHeaderValue;
  }

  @Override
  public String toString() {
    return httpHeaderValue;
  }
}
