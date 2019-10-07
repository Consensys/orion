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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.exception.OrionException;
import net.consensys.orion.http.server.HttpContentType;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SendRequestParserTest {

  private RoutingContext routingContext;
  private SendRequestParser parser;

  @BeforeEach
  public void beforeEach() {
    routingContext = mock(RoutingContext.class);
  }

  @Test
  public void successfulJsonParse() {
    parser = new SendRequestParser(HttpContentType.JSON);
    final SendRequest expectedSendRequest = sendRequest();
    when(routingContext.getBody()).thenReturn(Json.encodeToBuffer(expectedSendRequest));

    final SendRequest sendRequest = parser.parse(routingContext);

    assertThat(sendRequest).isEqualToComparingFieldByField(expectedSendRequest);
  }

  @Test
  public void successfulOctetParse() {
    parser = new SendRequestParser(HttpContentType.APPLICATION_OCTET_STREAM);

    final SendRequest expectedSendRequest = sendRequest();

    HttpServerRequest request = mock(HttpServerRequest.class);
    when(request.getHeader("c11n-from")).thenReturn(expectedSendRequest.from().get());
    when(request.getHeader("c11n-to")).thenReturn(String.join(",", expectedSendRequest.to()));
    when(routingContext.request()).thenReturn(request);
    when(routingContext.getBody()).thenReturn(Buffer.buffer("Hello, World!"));

    final SendRequest sendRequest = parser.parse(routingContext);

    assertThat(sendRequest).isEqualToComparingFieldByField(expectedSendRequest);
  }

  @Test
  public void unsupportedObjectTypeShouldThrowException() {
    parser = new SendRequestParser(HttpContentType.TEXT);

    OrionException exception = assertThrows(OrionException.class, () -> parser.parse(routingContext));

    assertThat(exception.code()).isEqualTo(OrionErrorCode.OBJECT_UNSUPPORTED_TYPE);
  }

  @Test
  public void invalidRequestShouldThrowException() {
    parser = new SendRequestParser(HttpContentType.JSON);

    final SendRequest request = new SendRequest(
        "",
        "DoZ4XxCxpHgAYyci0qyffXf1rpynUDnC9VDZoANI+Wc=",
        new String[] {"DoZ4XxCxpHgAYyci0qyffXf1rpynUDnC9VDZoANI+Wc="});

    when(routingContext.getBody()).thenReturn(Json.encodeToBuffer(request));

    OrionException exception = assertThrows(OrionException.class, () -> parser.parse(routingContext));

    assertThat(exception.code()).isEqualTo(OrionErrorCode.INVALID_PAYLOAD);
  }

  @Test
  public void requestWithNullToUsesFrom() {
    parser = new SendRequestParser(HttpContentType.JSON);

    final SendRequest expectedSendRequest = new SendRequest(
        "SGVsbG8sIFdvcmxkIQ==",
        "DoZ4XxCxpHgAYyci0qyffXf1rpynUDnC9VDZoANI+Wc=",
        new String[] {"DoZ4XxCxpHgAYyci0qyffXf1rpynUDnC9VDZoANI+Wc="});

    final SendRequest request =
        new SendRequest("SGVsbG8sIFdvcmxkIQ==", "DoZ4XxCxpHgAYyci0qyffXf1rpynUDnC9VDZoANI+Wc=", null);

    when(routingContext.getBody()).thenReturn(Json.encodeToBuffer(request));

    assertThat(parser.parse(routingContext)).isEqualToComparingFieldByField(expectedSendRequest);
  }

  @Test
  public void requestWithEmptyToUsesFrom() {
    parser = new SendRequestParser(HttpContentType.JSON);

    final SendRequest expectedSendRequest = new SendRequest(
        "SGVsbG8sIFdvcmxkIQ==",
        "DoZ4XxCxpHgAYyci0qyffXf1rpynUDnC9VDZoANI+Wc=",
        new String[] {"DoZ4XxCxpHgAYyci0qyffXf1rpynUDnC9VDZoANI+Wc="});

    final SendRequest request =
        new SendRequest("SGVsbG8sIFdvcmxkIQ==", "DoZ4XxCxpHgAYyci0qyffXf1rpynUDnC9VDZoANI+Wc=", new String[] {});

    when(routingContext.getBody()).thenReturn(Json.encodeToBuffer(request));

    assertThat(parser.parse(routingContext)).isEqualToComparingFieldByField(expectedSendRequest);
  }

  private SendRequest sendRequest() {
    return sendRequest(
        new String[] {"DoZ4XxCxpHgAYyci0qyffXf1rpynUDnC9VDZoANI+Wc=", "GGilEkXLaQ9yhhtbpBT03Me9iYa7U/mWXxrJhnbl1XY="});
  }

  private SendRequest sendRequest(String[] to) {
    // Payload: Hello, World!
    return new SendRequest("SGVsbG8sIFdvcmxkIQ==", "DoZ4XxCxpHgAYyci0qyffXf1rpynUDnC9VDZoANI+Wc=", to);
  }
}
