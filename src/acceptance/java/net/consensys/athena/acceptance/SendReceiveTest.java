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
import org.junit.Before;
import org.junit.Test;

public class SendReceiveTest {

  private final String configFilePath =
      this.getClass().getClassLoader().getResource("singlenode.conf").getPath();

  private static final String baseUrl = "http://127.0.0.1:9001";
  private static final OkHttpClient httpClient = new OkHttpClient();
  private static final Request upRequest =
      new Request.Builder().get().url(baseUrl + AthenaRoutes.UPCHECK).build();
  private static final Serializer serializer = new Serializer();

  private static final String pk1b64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String pk2b64 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";

  @Before
  public void setUp() throws Exception {
    // setup a single node with 2 public keys
    // generate public / private keys
    Athena athena = new Athena();
    //    athena.run(new String[] {"-g", "key1"});
    //    athena.run(new String[] {"-g", "key2"});
    athena.run(new String[] {configFilePath});
  }

  @Test
  public void testSingleNode() throws Exception {
    // ensure the node is awake
    Response upResponse = httpClient.newCall(upRequest).execute();
    assertEquals(200, upResponse.code());
    assertEquals("I'm up!", upResponse.body().string());

    byte[] originalPayload =
        ("pragma solidity ^0.4.17;\n"
                + "\n"
                + "contract SimpleStorage {\n"
                + "  uint public storedData;\n"
                + "\n"
                + "  function SimpleStorage(uint initVal) public {\n"
                + "    storedData = initVal;\n"
                + "  }\n"
                + "\n"
                + "  function set(uint x) public {\n"
                + "    storedData = x;\n"
                + "  }\n"
                + "\n"
                + "  function get() constant public returns (uint retVal) {\n"
                + "    return storedData;\n"
                + "  }\n"
                + "}")
            .getBytes();

    // send something to the node (from pk1 to pk2)
    SendRequest sendRequest =
        new SendRequest(Base64.encode(originalPayload), pk1b64, new String[] {pk2b64});
    RequestBody sendBody =
        RequestBody.create(
            MediaType.parse(JSON.httpHeaderValue), serializer.serialize(JSON, sendRequest));

    Request httpSendRequest =
        new Request.Builder().post(sendBody).url(baseUrl + AthenaRoutes.SEND).build();
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
        new Request.Builder().post(receiveBody).url(baseUrl + AthenaRoutes.RECIEVE).build();
    Response httpReceiveResponse = httpClient.newCall(httpReceiveRequest).execute();
    assertEquals(200, httpReceiveResponse.code());

    // deserialize the response
    ReceiveResponse receiveResponse =
        serializer.deserialize(JSON, ReceiveResponse.class, httpReceiveResponse.body().bytes());
    byte[] receivedPayload = Base64.decode(receiveResponse.payload);
    assertArrayEquals(originalPayload, receivedPayload);
  }
}
