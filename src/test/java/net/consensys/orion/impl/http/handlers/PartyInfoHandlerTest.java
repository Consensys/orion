package net.consensys.orion.impl.http.handlers;

import static junit.framework.TestCase.assertTrue;
import static net.consensys.orion.impl.http.server.HttpContentType.CBOR;
import static net.consensys.orion.impl.http.server.HttpContentType.JSON;
import static org.junit.Assert.assertEquals;

import net.consensys.orion.api.cmd.OrionRoutes;
import net.consensys.orion.api.network.NetworkNodes;
import net.consensys.orion.impl.enclave.sodium.SodiumPublicKey;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.network.MemoryNetworkNodes;

import java.net.URL;

import junit.framework.TestCase;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.Test;

public class PartyInfoHandlerTest extends HandlerTest {

  @Test
  public void testSuccessfulProcessingOfRequest() throws Exception {
    networkNodes.addNode(new SodiumPublicKey("pk1".getBytes()), new URL("http://127.0.0.1:9001/"));
    networkNodes.addNode(new SodiumPublicKey("pk2".getBytes()), new URL("http://127.0.0.1:9002/"));

    // prepare /partyinfo payload (our known peers)
    RequestBody partyInfoBody =
        RequestBody.create(
            MediaType.parse(CBOR.httpHeaderValue), serializer.serialize(CBOR, networkNodes));

    // call http endpoint
    Request request =
        new Request.Builder().post(partyInfoBody).url(baseUrl + OrionRoutes.PARTYINFO).build();

    Response resp = httpClient.newCall(request).execute();
    assertEquals(200, resp.code());

    NetworkNodes partyInfoResponse =
        serializer.deserialize(HttpContentType.CBOR, MemoryNetworkNodes.class, resp.body().bytes());

    assertEquals(networkNodes, partyInfoResponse);
  }

  @Test
  public void testRoundTripSerialization() throws Exception {
    MemoryNetworkNodes networkNodes = new MemoryNetworkNodes(new URL("http://localhost:1234/"));
    networkNodes.addNode(new SodiumPublicKey("fake".getBytes()), new URL("http://localhost/"));
    assertEquals(
        networkNodes,
        serializer.roundTrip(HttpContentType.CBOR, MemoryNetworkNodes.class, networkNodes));
    assertEquals(
        networkNodes,
        serializer.roundTrip(HttpContentType.JSON, MemoryNetworkNodes.class, networkNodes));
  }

  @Test
  public void testPartyInfoWithInvalidContentType() throws Exception {
    networkNodes.addNode(new SodiumPublicKey("pk1".getBytes()), new URL("http://127.0.0.1:9001/"));
    networkNodes.addNode(new SodiumPublicKey("pk2".getBytes()), new URL("http://127.0.0.1:9002/"));

    // prepare /partyinfo payload (our known peers) with invalid content type (json)
    RequestBody partyInfoBody =
        RequestBody.create(
            MediaType.parse(JSON.httpHeaderValue), serializer.serialize(JSON, networkNodes));

    Request request =
        new Request.Builder().post(partyInfoBody).url(baseUrl + OrionRoutes.PARTYINFO).build();

    Response resp = httpClient.newCall(request).execute();
    assertEquals(404, resp.code());
  }

  @Test
  public void testPartyInfoWithInvalidBody() throws Exception {
    RequestBody partyInfoBody = RequestBody.create(MediaType.parse(CBOR.httpHeaderValue), "foo");

    Request request =
        new Request.Builder().post(partyInfoBody).url(baseUrl + OrionRoutes.PARTYINFO).build();

    Response resp = httpClient.newCall(request).execute();

    // produces 500 because serialisation error
    TestCase.assertEquals(500, resp.code());
    // checks if the failure reason was with de-serialisation
    assertTrue(resp.body().string().contains("com.fasterxml.jackson"));
  }
}
