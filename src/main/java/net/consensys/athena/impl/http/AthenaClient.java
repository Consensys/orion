package net.consensys.athena.impl.http;

import static net.consensys.athena.impl.http.server.HttpContentType.JSON;

import net.consensys.athena.api.cmd.AthenaRoutes;
import net.consensys.athena.impl.http.handler.receive.ReceiveRequest;
import net.consensys.athena.impl.http.handler.receive.ReceiveResponse;
import net.consensys.athena.impl.http.handler.send.SendRequest;
import net.consensys.athena.impl.http.handler.send.SendResponse;
import net.consensys.athena.impl.utils.Base64;
import net.consensys.athena.impl.utils.Serializer;

import java.io.IOException;
import java.util.Optional;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AthenaClient {
  private static final Logger log = LogManager.getLogger();

  private final OkHttpClient httpClient = new OkHttpClient();
  private final Serializer serializer = new Serializer();

  private final String baseUrl;
  private final Request upRequest;

  public AthenaClient(String baseUrl) {
    this.baseUrl = baseUrl;

    // setup immutable upCheck request
    upRequest = new Request.Builder().get().url(baseUrl + AthenaRoutes.UPCHECK).build();
  }

  public boolean upCheck() {
    try (Response upResponse = httpClient.newCall(upRequest).execute()) {
      return upResponse.code() == 200;
    } catch (IOException io) {
      return false;
    }
  }

  public Optional<String> send(byte[] payload, String from, String[] to) {
    // create the okHttp Request object
    SendRequest sendRequest = new SendRequest(Base64.encode(payload), from, to);
    RequestBody sendBody =
        RequestBody.create(
            MediaType.parse(JSON.httpHeaderValue), serializer.serialize(JSON, sendRequest));

    Request httpSendRequest =
        new Request.Builder().post(sendBody).url(baseUrl + AthenaRoutes.SEND).build();

    // executes the request
    try (Response httpSendResponse = httpClient.newCall(httpSendRequest).execute()) {
      if (httpSendResponse.code() != 200) {
        log.error("send operation failed " + httpSendResponse.code());
        return Optional.empty();
      }

      // deserialize the response
      SendResponse sendResponse =
          serializer.deserialize(JSON, SendResponse.class, httpSendResponse.body().bytes());
      return Optional.of(sendResponse.key);
    } catch (IOException io) {
      log.error(io.getMessage());
      return Optional.empty();
    }
  }

  public Optional<byte[]> receive(String digest, String publicKey) {
    // create the okHttp Request object
    ReceiveRequest receiveRequest = new ReceiveRequest(digest, publicKey);
    RequestBody receiveBody =
        RequestBody.create(
            MediaType.parse(JSON.httpHeaderValue), serializer.serialize(JSON, receiveRequest));

    Request httpReceiveRequest =
        new Request.Builder().post(receiveBody).url(baseUrl + AthenaRoutes.RECIEVE).build();

    // executes the request
    try (Response httpReceiveResponse = httpClient.newCall(httpReceiveRequest).execute()) {
      if (httpReceiveResponse.code() != 200) {
        log.error("receive operation failed " + httpReceiveResponse.code());
        return Optional.empty();
      }

      // deserialize the response
      ReceiveResponse receiveResponse =
          serializer.deserialize(JSON, ReceiveResponse.class, httpReceiveResponse.body().bytes());

      return Optional.of(Base64.decode(receiveResponse.payload));

    } catch (IOException io) {
      log.error(io.getMessage());
      return Optional.empty();
    }
  }
}
