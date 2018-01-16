package net.consensys.athena.acceptance;

import static net.consensys.athena.impl.http.server.HttpContentType.JSON;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import net.consensys.athena.api.cmd.Athena;
import net.consensys.athena.api.cmd.AthenaRoutes;
import net.consensys.athena.impl.http.handler.receive.ReceiveRequest;
import net.consensys.athena.impl.http.handler.receive.ReceiveResponse;
import net.consensys.athena.impl.http.handler.send.SendRequest;
import net.consensys.athena.impl.http.handler.send.SendResponse;
import net.consensys.athena.impl.utils.Base64;
import net.consensys.athena.impl.utils.Serializer;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.Test;

public class SendReceiveTest {

  private static final byte[] originalPayload = "a wonderful transaction".getBytes();

  private final String singleNodeConfig =
      getClass().getClassLoader().getResource("singlenode.conf").getPath();
  private final String node1Config =
      getClass().getClassLoader().getResource("node1.conf").getPath();
  private final String node2Config =
      getClass().getClassLoader().getResource("node2.conf").getPath();

  private static final String singleNodeBaseUrl = "http://127.0.0.1:9001";
  private static final String node1BaseUrl = "http://127.0.0.1:9002";
  private static final String node2BaseUrl = "http://127.0.0.1:9003";

  private static final OkHttpClient httpClient = new OkHttpClient();
  private static final Serializer serializer = new Serializer();

  private static final String pk1b64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String pk2b64 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";

  @Test
  public void testSingleNode() throws Exception {
    // setup a single node with 2 public keys
    Athena athena = new Athena();
    athena.run(new String[] {singleNodeConfig});

    // ensure the node is awake
    ensureNodeIsAwake(singleNodeBaseUrl);

    // send something to the node (from pk1 to pk2)
    SendRequest sendRequest =
        new SendRequest(Base64.encode(originalPayload), pk1b64, new String[] {pk2b64});
    RequestBody sendBody =
        RequestBody.create(
            MediaType.parse(JSON.httpHeaderValue), serializer.serialize(JSON, sendRequest));

    Request httpSendRequest =
        new Request.Builder().post(sendBody).url(singleNodeBaseUrl + AthenaRoutes.SEND).build();
    Response httpSendResponse = httpClient.newCall(httpSendRequest).execute();
    assertEquals(200, httpSendResponse.code());

    // deserialize the response
    SendResponse sendResponse =
        serializer.deserialize(JSON, SendResponse.class, httpSendResponse.body().bytes());

    // call receive on the node
    ReceiveRequest receiveRequest = new ReceiveRequest(sendResponse.key, pk2b64);
    RequestBody receiveBody =
        RequestBody.create(
            MediaType.parse(JSON.httpHeaderValue), serializer.serialize(JSON, receiveRequest));

    Request httpReceiveRequest =
        new Request.Builder()
            .post(receiveBody)
            .url(singleNodeBaseUrl + AthenaRoutes.RECIEVE)
            .build();
    Response httpReceiveResponse = httpClient.newCall(httpReceiveRequest).execute();
    assertEquals(200, httpReceiveResponse.code());

    // deserialize the response
    ReceiveResponse receiveResponse =
        serializer.deserialize(JSON, ReceiveResponse.class, httpReceiveResponse.body().bytes());
    byte[] receivedPayload = Base64.decode(receiveResponse.payload);
    assertArrayEquals(originalPayload, receivedPayload);
  }

  @Test
  public void testTwoNodes() throws Exception {
    // setup our 2 nodes
    Athena node1 = new Athena();
    node1.run(new String[] {node1Config});
    ensureNodeIsAwake(node1BaseUrl);

    Athena node2 = new Athena();
    node2.run(new String[] {node2Config});
    ensureNodeIsAwake(node2BaseUrl);

    // ensure network discovery ran on node 1
    Thread.sleep(1000);

    // send a transaction from node1 to node2
    SendRequest sendRequest =
        new SendRequest(Base64.encode(originalPayload), pk1b64, new String[] {pk2b64});
    RequestBody sendBody =
        RequestBody.create(
            MediaType.parse(JSON.httpHeaderValue), serializer.serialize(JSON, sendRequest));

    Request httpSendRequest =
        new Request.Builder().post(sendBody).url(node1BaseUrl + AthenaRoutes.SEND).build();
    Response httpSendResponse = httpClient.newCall(httpSendRequest).execute();
    assertEquals(200, httpSendResponse.code());
    // deserialize the response
    SendResponse sendResponse =
        serializer.deserialize(JSON, SendResponse.class, httpSendResponse.body().bytes());

    // call receive on the node 2
    ReceiveRequest receiveRequest = new ReceiveRequest(sendResponse.key, pk2b64);
    RequestBody receiveBody =
        RequestBody.create(
            MediaType.parse(JSON.httpHeaderValue), serializer.serialize(JSON, receiveRequest));

    Request httpReceiveRequest =
        new Request.Builder().post(receiveBody).url(node2BaseUrl + AthenaRoutes.RECIEVE).build();
    Response httpReceiveResponse = httpClient.newCall(httpReceiveRequest).execute();
    assertEquals(200, httpReceiveResponse.code());

    // deserialize the response
    ReceiveResponse receiveResponse =
        serializer.deserialize(JSON, ReceiveResponse.class, httpReceiveResponse.body().bytes());
    byte[] receivedPayload = Base64.decode(receiveResponse.payload);

    assertArrayEquals(originalPayload, receivedPayload);
  }

  private void ensureNodeIsAwake(String baseUrl) throws Exception {
    Request upRequest = new Request.Builder().get().url(baseUrl + AthenaRoutes.UPCHECK).build();
    Response upResponse = httpClient.newCall(upRequest).execute();
    assertEquals(200, upResponse.code());
    assertEquals("I'm up!", upResponse.body().string());
  }
}
