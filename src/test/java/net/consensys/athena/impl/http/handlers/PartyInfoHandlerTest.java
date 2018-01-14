package net.consensys.athena.impl.http.handlers;

import static org.junit.Assert.assertEquals;

import net.consensys.athena.api.cmd.AthenaRoutes;
import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.impl.http.server.HttpContentType;
import net.consensys.athena.impl.network.MemoryNetworkNodes;

import java.net.URL;

import okhttp3.Request;
import okhttp3.Response;
import org.junit.Test;

public class PartyInfoHandlerTest extends HandlerTest {

  @Test
  public void testSuccessfulProcessingOfRequest() throws Exception {

    networkNodes.addNode(new SodiumPublicKey("pk1".getBytes()), new URL("http://127.0.0.1:9001/"));
    networkNodes.addNode(new SodiumPublicKey("pk2".getBytes()), new URL("http://127.0.0.1:9002/"));

    Request request = new Request.Builder().get().url(baseUrl + AthenaRoutes.PARTYINFO).build();

    Response resp = httpClient.newCall(request).execute();
    assertEquals(200, resp.code());

    NetworkNodes partyInfoResponse =
        serializer.deserialize(HttpContentType.CBOR, MemoryNetworkNodes.class, resp.body().bytes());

    assertEquals(networkNodes, partyInfoResponse);
  }

  @Test
  public void testRoundTripSerialization() throws Exception {
    MemoryNetworkNodes networkNodes = new MemoryNetworkNodes();
    networkNodes.addNode(new SodiumPublicKey("fake".getBytes()), new URL("http://localhost/"));
    assertEquals(
        networkNodes,
        serializer.roundTrip(HttpContentType.CBOR, MemoryNetworkNodes.class, networkNodes));
    assertEquals(
        networkNodes,
        serializer.roundTrip(HttpContentType.JSON, MemoryNetworkNodes.class, networkNodes));
  }
}
