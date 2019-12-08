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

import static net.consensys.cava.io.Base64.decodeBytes;
import static net.consensys.orion.http.server.HttpContentType.JSON;

import net.consensys.orion.http.handler.privacy.DeletePrivacyGroupRequest;
import net.consensys.orion.http.handler.privacy.FindPrivacyGroupRequest;
import net.consensys.orion.http.handler.privacy.PrivacyGroup;
import net.consensys.orion.http.handler.privacy.PrivacyGroupRequest;
import net.consensys.orion.http.handler.privacy.RetrievePrivacyGroupRequest;
import net.consensys.orion.http.handler.receive.ReceiveRequest;
import net.consensys.orion.http.handler.receive.ReceiveResponse;
import net.consensys.orion.utils.Serializer;

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

  EthClientStub(final int clientPort, final HttpClient httpClient) {
    this.httpClient = httpClient;
    this.clientPort = clientPort;
  }

  public boolean upCheck() {
    final CompletableFuture<Integer> statusCodeFuture = new CompletableFuture<>();
    httpClient
        .get(clientPort, "localhost", "/upcheck")
        .handler(resp -> statusCodeFuture.complete(resp.statusCode()))
        .exceptionHandler(statusCodeFuture::completeExceptionally)
        .end();
    return Integer.valueOf(200).equals(statusCodeFuture.join());
  }

  Optional<String> send(final byte[] payload, final String from, final String[] to) {
    final Map<String, Object> sendRequest = sendRequest(payload, from, to);
    final CompletableFuture<String> keyFuture = new CompletableFuture<>();
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

  public Optional<String> sendExpectingError(final byte[] payload, final String from, final String[] to) {
    final Map<String, Object> sendRequest = sendRequest(payload, from, to);
    final CompletableFuture<String> keyFuture = new CompletableFuture<>();
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

  Optional<String> send(final byte[] payload, final String from, final String privacyGroupId) {
    final Map<String, Object> sendRequest = sendRequest(payload, from, privacyGroupId);
    final CompletableFuture<String> keyFuture = new CompletableFuture<>();
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

  public Optional<String> sendPrivacyExpectingError(
      final byte[] payload,
      final String from,
      final String privacyGroupId) {
    final Map<String, Object> sendRequest = sendRequest(payload, from, privacyGroupId);
    final CompletableFuture<String> keyFuture = new CompletableFuture<>();
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

  Optional<byte[]> receive(final String digest, final String publicKey) {
    final ReceiveRequest receiveRequest = new ReceiveRequest(digest, publicKey);
    final CompletableFuture<byte[]> payloadFuture = new CompletableFuture<>();
    httpClient.post(clientPort, "localhost", "/receive").handler(resp -> {
      if (resp.statusCode() == 200) {
        resp.bodyHandler(body -> payloadFuture.complete(decodeBytes(deserialize(body).get("payload"))));
      } else {
        payloadFuture.complete(null);
      }
    }).exceptionHandler(payloadFuture::completeExceptionally).putHeader("Content-Type", "application/json").end(
        Buffer.buffer(Serializer.serialize(JSON, receiveRequest)));
    return Optional.ofNullable(payloadFuture.join());
  }

  ReceiveResponse receivePrivacy(final String digest, final String publicKey) {
    final ReceiveRequest receiveRequest = new ReceiveRequest(digest, publicKey);
    final CompletableFuture<ReceiveResponse> payloadFuture = new CompletableFuture<>();
    httpClient.post(clientPort, "localhost", "/receive").handler(resp -> {
      if (resp.statusCode() == 200) {
        resp.bodyHandler(body -> payloadFuture.complete(deserialize(body, ReceiveResponse.class)));
      } else {
        payloadFuture.complete(null);
      }
    })
        .exceptionHandler(payloadFuture::completeExceptionally)
        .putHeader("Content-Type", "application/vnd.orion.v1+json")
        .end(Buffer.buffer(Serializer.serialize(JSON, receiveRequest)));
    return payloadFuture.join();
  }

  public Optional<PrivacyGroup> createPrivacyGroup(
      final String[] addresses,
      final String from,
      final String name,
      final String description) {
    final PrivacyGroupRequest createGroupRequest = new PrivacyGroupRequest(addresses, from, name, description);
    final CompletableFuture<PrivacyGroup> keyFuture = new CompletableFuture<>();
    httpClient.post(clientPort, "localhost", "/createPrivacyGroup").handler(resp -> {
      if (resp.statusCode() == 200) {
        resp.bodyHandler(body -> keyFuture.complete(deserialize(body, PrivacyGroup.class)));
      } else {
        keyFuture.complete(null);
      }
    }).exceptionHandler(keyFuture::completeExceptionally).putHeader("Content-Type", "application/json").end(
        Buffer.buffer(Serializer.serialize(JSON, createGroupRequest)));
    return Optional.ofNullable(keyFuture.join());
  }

  public Optional<PrivacyGroup[]> findPrivacyGroup(final String[] addresses) {
    final FindPrivacyGroupRequest findGroupRequest = new FindPrivacyGroupRequest(addresses);
    final CompletableFuture<PrivacyGroup[]> keyFuture = new CompletableFuture<>();
    httpClient.post(clientPort, "localhost", "/findPrivacyGroup").handler(resp -> {
      if (resp.statusCode() == 200) {
        resp.bodyHandler(body -> keyFuture.complete(deserialize(body, PrivacyGroup[].class)));
      } else {
        keyFuture.complete(null);
      }
    }).exceptionHandler(keyFuture::completeExceptionally).putHeader("Content-Type", "application/json").end(
        Buffer.buffer(Serializer.serialize(JSON, findGroupRequest)));
    return Optional.ofNullable(keyFuture.join());
  }

  public Optional<PrivacyGroup> retrievePrivacyGroup(final String privacyGroupId) {
    final RetrievePrivacyGroupRequest getGroupRequest = new RetrievePrivacyGroupRequest(privacyGroupId);
    final CompletableFuture<PrivacyGroup> keyFuture = new CompletableFuture<>();
    httpClient.post(clientPort, "localhost", "/retrievePrivacyGroup").handler(resp -> {
      if (resp.statusCode() == 200) {
        resp.bodyHandler(body -> keyFuture.complete(deserialize(body, PrivacyGroup.class)));
      } else {
        keyFuture.complete(null);
      }
    }).exceptionHandler(keyFuture::completeExceptionally).putHeader("Content-Type", "application/json").end(
        Buffer.buffer(Serializer.serialize(JSON, getGroupRequest)));
    return Optional.ofNullable(keyFuture.join());
  }

  public Optional<String> deletePrivacyGroup(final String privacyGroupId, final String from) {
    final DeletePrivacyGroupRequest deleteGroupRequest = new DeletePrivacyGroupRequest(privacyGroupId, from);
    final CompletableFuture<String> keyFuture = new CompletableFuture<>();
    httpClient.post(clientPort, "localhost", "/deletePrivacyGroup").handler(resp -> {
      if (resp.statusCode() == 200) {
        resp.bodyHandler((body) -> {
          String res = deserialize(body, String.class);
          keyFuture.complete(res);
        });
      } else {
        keyFuture.complete(null);
      }
    }).exceptionHandler(keyFuture::completeExceptionally).putHeader("Content-Type", "application/json").end(
        Buffer.buffer(Serializer.serialize(JSON, deleteGroupRequest)));
    return Optional.ofNullable(keyFuture.join());
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> deserialize(final Buffer httpSendResponse) {
    return Serializer.deserialize(JSON, Map.class, httpSendResponse.getBytes());
  }

  @SuppressWarnings("unchecked")
  private <T> T deserialize(final Buffer httpSendResponse, final Class<T> responseType) {
    return Serializer.deserialize(JSON, responseType, httpSendResponse.getBytes());
  }

  private Map<String, Object> sendRequest(final byte[] payload, final String from, final String[] to) {
    final Map<String, Object> map = new HashMap<>();
    map.put("payload", payload);
    map.put("from", from);
    map.put("to", to);
    return map;
  }

  private Map<String, Object> sendRequest(final byte[] payload, final String from, final String privacyGroupId) {
    final Map<String, Object> map = new HashMap<>();
    map.put("payload", payload);
    map.put("from", from);
    map.put("privacyGroupId", privacyGroupId);
    return map;
  }
}
