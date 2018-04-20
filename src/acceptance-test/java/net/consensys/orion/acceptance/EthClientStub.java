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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Simple Ethereum Client for calling Orion APIs */
public class EthClientStub {
  private static final Logger log = LogManager.getLogger();

  private final HttpClient httpClient;
  private final int clientPort;

  public EthClientStub(int clientPort, HttpClient httpClient) {
    this.httpClient = httpClient;
    this.clientPort = clientPort;
  }

  public boolean upCheck() {
    CompletableFuture<Integer> statusCodeFuture = new CompletableFuture<>();
    httpClient
        .get(clientPort, "localhost", "/upcheck")
        .handler(resp -> statusCodeFuture.complete(resp.statusCode()))
        .exceptionHandler(statusCodeFuture::completeExceptionally)
        .end();
    return Integer.valueOf(200).equals(statusCodeFuture.join());
  }

  public Optional<String> send(byte[] payload, String from, String[] to) {
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

  public Optional<byte[]> receive(String digest, String publicKey) {
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
