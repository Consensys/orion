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
import static net.consensys.orion.http.server.HttpContentType.CBOR;
import static net.consensys.orion.http.server.HttpContentType.JSON;

import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.http.handler.privacy.DeletePrivacyGroupRequest;
import net.consensys.orion.http.handler.privacy.FindPrivacyGroupRequest;
import net.consensys.orion.http.handler.privacy.PrivacyGroup;
import net.consensys.orion.http.handler.privacy.PrivacyGroupRequest;
import net.consensys.orion.http.handler.receive.ReceiveRequest;
import net.consensys.orion.http.handler.receive.ReceiveResponse;
import net.consensys.orion.http.handler.tx.PushToHistoryRequest;
import net.consensys.orion.utils.Serializer;

import java.nio.charset.StandardCharsets;
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

  Optional<String> send(byte[] payload, String from, String privacyGroupId) {
    Map<String, Object> sendRequest = sendRequest(payload, from, privacyGroupId);
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

  public Optional<String> sendPrivacyExpectingError(byte[] payload, String from, String privacyGroupId) {
    Map<String, Object> sendRequest = sendRequest(payload, from, privacyGroupId);
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
        resp.bodyHandler(body -> payloadFuture.complete(decodeBytes(deserialize(body).get("payload"))));
      } else {
        payloadFuture.complete(null);
      }
    }).exceptionHandler(payloadFuture::completeExceptionally).putHeader("Content-Type", "application/json").end(
        Buffer.buffer(Serializer.serialize(JSON, receiveRequest)));
    return Optional.ofNullable(payloadFuture.join());
  }

  ReceiveResponse receivePrivacy(String digest, String publicKey) {
    ReceiveRequest receiveRequest = new ReceiveRequest(digest, publicKey);
    CompletableFuture<ReceiveResponse> payloadFuture = new CompletableFuture<>();
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

  public Optional<PrivacyGroup> createPrivacyGroup(String[] addresses, String from, String name, String description) {
    PrivacyGroupRequest createGroupRequest = new PrivacyGroupRequest(addresses, from, name, description);
    CompletableFuture<PrivacyGroup> keyFuture = new CompletableFuture<>();
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

  public Optional<PrivacyGroup[]> findPrivacyGroup(String[] addresses) {
    FindPrivacyGroupRequest findGroupRequest = new FindPrivacyGroupRequest(addresses);
    CompletableFuture<PrivacyGroup[]> keyFuture = new CompletableFuture<>();
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

  public Optional<String> deletePrivacyGroup(String privacyGroupId, String from) {
    DeletePrivacyGroupRequest deleteGroupRequest = new DeletePrivacyGroupRequest(privacyGroupId, from);
    CompletableFuture<String> keyFuture = new CompletableFuture<>();
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


  public Optional<String> push(EncryptedPayload payload) {
    CompletableFuture<String> pushFuture = new CompletableFuture<>();
    httpClient.post(clientPort, "localhost", "/push").handler(resp -> {
      if (resp.statusCode() == 200) {
        resp.bodyHandler((body) -> {
          pushFuture.complete(new String(body.getBytes(), StandardCharsets.UTF_8));
        });
      } else {
        pushFuture.complete(null);
      }
    }).exceptionHandler(pushFuture::completeExceptionally).putHeader("Content-Type", "application/cbor").end(
        Buffer.buffer(Serializer.serialize(CBOR, payload)));
    return Optional.ofNullable(pushFuture.join());
  }


  public Optional<Boolean> pushToHistory(
      String privacyGroupId,
      String privacyMarkerTransactionHash,
      String enclaveKey) {
    PushToHistoryRequest pushToHistoryRequest =
        new PushToHistoryRequest(privacyGroupId, privacyMarkerTransactionHash, enclaveKey);
    CompletableFuture<Boolean> keyFuture = new CompletableFuture<>();
    httpClient.post(clientPort, "localhost", "/pushToHistory").handler(resp -> {
      if (resp.statusCode() == 200) {
        resp.bodyHandler((body) -> {
          Boolean res = deserialize(body, Boolean.class);
          keyFuture.complete(res);
        });
      } else {
        keyFuture.complete(null);
      }
    }).exceptionHandler(keyFuture::completeExceptionally).putHeader("Content-Type", "application/json").end(
        Buffer.buffer(Serializer.serialize(JSON, pushToHistoryRequest)));
    return Optional.ofNullable(keyFuture.join());
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> deserialize(Buffer httpSendResponse) {
    return Serializer.deserialize(JSON, Map.class, httpSendResponse.getBytes());
  }

  @SuppressWarnings("unchecked")
  private <T> T deserialize(Buffer httpSendResponse, Class<T> responseType) {
    return Serializer.deserialize(JSON, responseType, httpSendResponse.getBytes());
  }

  private Map<String, Object> sendRequest(byte[] payload, String from, String[] to) {
    Map<String, Object> map = new HashMap<>();
    map.put("payload", payload);
    map.put("from", from);
    map.put("to", to);
    return map;
  }

  private Map<String, Object> sendRequest(byte[] payload, String from, String privacyGroupId) {
    Map<String, Object> map = new HashMap<>();
    map.put("payload", payload);
    map.put("from", from);
    map.put("privacyGroupId", privacyGroupId);
    return map;
  }
}
