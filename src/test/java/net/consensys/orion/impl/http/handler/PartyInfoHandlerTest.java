package net.consensys.orion.impl.http.handler;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.orion.impl.http.server.HttpContentType.CBOR;
import static net.consensys.orion.impl.http.server.HttpContentType.JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.impl.enclave.sodium.SodiumPublicKey;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.network.ConcurrentNetworkNodes;
import net.consensys.orion.impl.utils.Serializer;

import java.net.URL;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

class PartyInfoHandlerTest extends HandlerTest {

  @Test
  void successfulProcessingOfRequest() throws Exception {
    networkNodes.addNode(new SodiumPublicKey("pk1".getBytes(UTF_8)), new URL("http://127.0.0.1:9001/"));
    networkNodes.addNode(new SodiumPublicKey("pk2".getBytes(UTF_8)), new URL("http://127.0.0.1:9002/"));

    // prepare /partyinfo payload (our known peers)
    RequestBody partyInfoBody =
        RequestBody.create(MediaType.parse(CBOR.httpHeaderValue), Serializer.serialize(CBOR, networkNodes));

    // call http endpoint
    Request request = new Request.Builder().post(partyInfoBody).url(nodeBaseUrl + "/partyinfo").build();

    Response resp = httpClient.newCall(request).execute();
    assertEquals(200, resp.code());

    ConcurrentNetworkNodes partyInfoResponse =
        Serializer.deserialize(HttpContentType.CBOR, ConcurrentNetworkNodes.class, resp.body().bytes());

    assertEquals(networkNodes, partyInfoResponse);
  }

  @Test
  void roundTripSerialization() throws Exception {
    ConcurrentNetworkNodes networkNodes = new ConcurrentNetworkNodes(new URL("http://localhost:1234/"));
    networkNodes.addNode(new SodiumPublicKey("fake".getBytes(UTF_8)), new URL("http://localhost/"));
    assertEquals(networkNodes, Serializer.roundTrip(HttpContentType.CBOR, ConcurrentNetworkNodes.class, networkNodes));
    assertEquals(networkNodes, Serializer.roundTrip(HttpContentType.JSON, ConcurrentNetworkNodes.class, networkNodes));
  }

  @Test
  void partyInfoWithInvalidContentType() throws Exception {
    networkNodes.addNode(new SodiumPublicKey("pk1".getBytes(UTF_8)), new URL("http://127.0.0.1:9001/"));
    networkNodes.addNode(new SodiumPublicKey("pk2".getBytes(UTF_8)), new URL("http://127.0.0.1:9002/"));

    // prepare /partyinfo payload (our known peers) with invalid content type (json)
    RequestBody partyInfoBody =
        RequestBody.create(MediaType.parse(JSON.httpHeaderValue), Serializer.serialize(JSON, networkNodes));

    Request request = new Request.Builder().post(partyInfoBody).url(nodeBaseUrl + "/partyinfo").build();

    Response resp = httpClient.newCall(request).execute();
    assertEquals(404, resp.code());
  }

  @Test
  void partyInfoWithInvalidBody() throws Exception {
    RequestBody partyInfoBody = RequestBody.create(MediaType.parse(CBOR.httpHeaderValue), "foo");

    Request request = new Request.Builder().post(partyInfoBody).url(nodeBaseUrl + "/partyinfo").build();

    Response resp = httpClient.newCall(request).execute();

    // produces 500 because serialisation error
    assertEquals(500, resp.code());
    // checks if the failure reason was with de-serialisation
    assertError(OrionErrorCode.OBJECT_JSON_DESERIALIZATION, resp);
  }
}
