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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AthenaClient {
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

  public String send(byte[] payload, String from, String[] to) {
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
        throw new RuntimeException("send operation failed: " + httpSendResponse.code());
      }

      // deserialize the response
      SendResponse sendResponse =
          serializer.deserialize(JSON, SendResponse.class, httpSendResponse.body().bytes());
      return sendResponse.key;
    } catch (IOException io) {
      throw new RuntimeException(io);
    }
  }

  public byte[] receive(String digest, String publicKey) {
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
        throw new RuntimeException("receive operation failed: " + httpReceiveResponse.code());
      }

      // deserialize the response
      ReceiveResponse receiveResponse =
          serializer.deserialize(JSON, ReceiveResponse.class, httpReceiveResponse.body().bytes());

      return Base64.decode(receiveResponse.payload);

    } catch (IOException io) {
      throw new RuntimeException(io);
    }
  }
}
