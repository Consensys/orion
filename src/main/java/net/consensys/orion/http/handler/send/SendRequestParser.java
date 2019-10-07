/*
 * Copyright 2019 ConsenSys AG.
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
package net.consensys.orion.http.handler.send;

import static net.consensys.orion.http.server.HttpContentType.JSON;

import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.exception.OrionException;
import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.utils.Serializer;

import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SendRequestParser {

  private static final Logger log = LogManager.getLogger();

  private final HttpContentType contentType;

  public SendRequestParser(HttpContentType contentType) {
    this.contentType = contentType;
  }

  SendRequest parse(final RoutingContext routingContext) {
    final SendRequest sendRequest;

    switch (contentType) {
      case JSON:
        sendRequest = parseFromJson(routingContext);
        break;
      case APPLICATION_OCTET_STREAM:
        sendRequest = parseFromOctetStream(routingContext);
        break;
      default:
        throw new OrionException(OrionErrorCode.OBJECT_UNSUPPORTED_TYPE);
    }

    log.debug(sendRequest);

    if ((sendRequest.to() == null || sendRequest.to().length == 0) && sendRequest.privacyGroupId().isEmpty()) {
      sendRequest.setTo(new String[] {sendRequest.from().orElse(null)});
    }

    if (!sendRequest.isValid()) {
      throw new OrionException(OrionErrorCode.INVALID_PAYLOAD);
    }

    return sendRequest;
  }

  private SendRequest parseFromJson(RoutingContext routingContext) {
    return Serializer.deserialize(JSON, SendRequest.class, routingContext.getBody().getBytes());
  }

  private SendRequest parseFromOctetStream(RoutingContext routingContext) {
    String from = routingContext.request().getHeader("c11n-from");
    String[] to = routingContext.request().getHeader("c11n-to").split(",");
    return new SendRequest(routingContext.getBody().getBytes(), from, to);
  }

  HttpContentType getContentType() {
    return contentType;
  }
}
