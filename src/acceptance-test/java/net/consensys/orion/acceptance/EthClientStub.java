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

package net.consensys.orion.acceptance;

import static net.consensys.orion.impl.http.server.HttpContentType.JSON;

import net.consensys.orion.impl.http.handler.receive.ReceiveRequest;
import net.consensys.orion.impl.utils.Base64;
import net.consensys.orion.impl.utils.Serializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;

/** Simple Ethereum Client for calling Orion APIs */
public class EthClientStub {
  private final HttpClient httpClient;
  private final int clientPort;

  EthClientStub(int clientPort, HttpClient httpClient) {
    this.httpClient = httpClient;
    this.clientPort = clientPort;
  }

  boolean upCheck() {
    CompletableFuture<Integer> statusCodeFuture = new CompletableFuture<>();
    httpClient
        .get(clientPort, "localhost", "/upcheck")
        .handler(resp -> statusCodeFuture.complete(resp.statusCode()))
        .exceptionHandler(statusCodeFuture::completeExceptionally)
        .end();
    return Integer.valueOf(200).equals(statusCodeFuture.join());
  }

  Optional<String> send(byte[] payload, String from, String[] to) {
    Map<String, Object> sendRequest = sendRequest(payload, from, to);
    CompletableFuture<String> keyFuture = new CompletableFuture<>();
    httpClient.post(clientPort, "localhost", "/send").handler(resp -> {
      if (resp.statusCode() == 200) {
        resp.bodyHandler(body -> keyFuture.complete(deserialize(body).get("key")));
      } else {
        keyFuture.complete(null);
      }
    }).exceptionHandler(keyFuture::completeExceptionally).putHeader("Content-Type", "application/json").end(
        Buffer.buffer(Serializer.serialize(JSON, sendRequest)));
    return Optional.ofNullable(keyFuture.join());
  }

  public Optional<String> sendExpectingError(byte[] payload, String from, String[] to) {
    Map<String, Object> sendRequest = sendRequest(payload, from, to);
    CompletableFuture<String> keyFuture = new CompletableFuture<>();
    httpClient.post(clientPort, "localhost", "/send").handler(resp -> {
      if (resp.statusCode() != 200) {
        resp.bodyHandler(body -> keyFuture.complete(body.toString()));
      } else {
        keyFuture.complete(null);
      }
    }).exceptionHandler(keyFuture::completeExceptionally).putHeader("Content-Type", "application/json").end(
        Buffer.buffer(Serializer.serialize(JSON, sendRequest)));
    return Optional.ofNullable(keyFuture.join());
  }

  Optional<byte[]> receive(String digest, String publicKey) {
    ReceiveRequest receiveRequest = new ReceiveRequest(digest, publicKey);
    CompletableFuture<byte[]> payloadFuture = new CompletableFuture<>();
    httpClient.post(clientPort, "localhost", "/receive").handler(resp -> {
      if (resp.statusCode() == 200) {
        resp.bodyHandler(body -> payloadFuture.complete(Base64.decode(deserialize(body).get("payload"))));
      } else {
        payloadFuture.complete(null);
      }
    }).exceptionHandler(payloadFuture::completeExceptionally).putHeader("Content-Type", "application/json").end(
        Buffer.buffer(Serializer.serialize(JSON, receiveRequest)));
    return Optional.ofNullable(payloadFuture.join());
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> deserialize(Buffer httpSendResponse) {
    return Serializer.deserialize(JSON, Map.class, httpSendResponse.getBytes());
  }

  private Map<String, Object> sendRequest(byte[] payload, String from, String[] to) {
    Map<String, Object> map = new HashMap<>();
    map.put("payload", payload);
    map.put("from", from);
    map.put("to", to);
    return map;
  }
}
