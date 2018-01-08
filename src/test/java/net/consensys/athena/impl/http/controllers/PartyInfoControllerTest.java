package net.consensys.athena.impl.http.controllers;

import static org.junit.Assert.assertEquals;

import net.consensys.athena.api.cmd.AthenaRoutes;
import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.network.MemoryNetworkNodes;

import java.net.URL;

import okhttp3.Request;
import okhttp3.Response;
import org.junit.Test;

public class PartyInfoControllerTest extends ControllerTest {

  @Test
  public void testSuccessfulProcessingOfRequest() throws Exception {
    networkNodes.addNodeURL(new URL("http://127.0.0.1:9001/"));
    networkNodes.addNodeURL(new URL("http://127.0.0.1:9002/"));

    Request request = new Request.Builder().get().url(baseUrl + AthenaRoutes.PARTYINFO).build();

    try (Response resp = httpClient.newCall(request).execute()) {
      assertEquals(200, resp.code());

      NetworkNodes partyInfoResponse =
          serializer.deserialize(ContentType.CBOR, MemoryNetworkNodes.class, resp.body().bytes());

      assertEquals(networkNodes, partyInfoResponse);
    }
  }
}
