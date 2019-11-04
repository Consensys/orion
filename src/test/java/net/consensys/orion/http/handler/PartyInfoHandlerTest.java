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
package net.consensys.orion.http.handler;

import static net.consensys.orion.http.server.HttpContentType.CBOR;
import static net.consensys.orion.http.server.HttpContentType.JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.network.ConcurrentNetworkNodes;
import net.consensys.orion.utils.Serializer;

import java.net.URL;
import java.util.Collections;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

class PartyInfoHandlerTest extends HandlerTest {

  @Test
  void successfulProcessingOfRequest() throws Exception {
    networkNodes
        .addNode(Collections.singletonList(Box.KeyPair.random().publicKey()), new URL("http://127.0.0.1:9001/"));
    networkNodes
        .addNode(Collections.singletonList(Box.KeyPair.random().publicKey()), new URL("http://127.0.0.1:9002/"));

    // prepare /partyinfo payload (our known peers)
    final RequestBody partyInfoBody =
        RequestBody.create(MediaType.parse(CBOR.httpHeaderValue), Serializer.serialize(CBOR, networkNodes));

    // call http endpoint
    final Request request = new Request.Builder().post(partyInfoBody).url(nodeBaseUrl + "/partyinfo").build();

    final Response resp = httpClient.newCall(request).execute();
    assertEquals(200, resp.code());

    final ConcurrentNetworkNodes partyInfoResponse =
        Serializer.deserialize(HttpContentType.CBOR, ConcurrentNetworkNodes.class, resp.body().bytes());

    assertEquals(networkNodes, partyInfoResponse);
  }

  @Test
  void roundTripSerialization() throws Exception {
    final ConcurrentNetworkNodes networkNodes = new ConcurrentNetworkNodes(new URL("http://localhost:1234/"));
    networkNodes.addNode(Collections.singletonList(Box.KeyPair.random().publicKey()), new URL("http://localhost/"));
    assertEquals(networkNodes, Serializer.roundTrip(HttpContentType.CBOR, ConcurrentNetworkNodes.class, networkNodes));
    assertEquals(networkNodes, Serializer.roundTrip(HttpContentType.JSON, ConcurrentNetworkNodes.class, networkNodes));
  }

  @Test
  void partyInfoWithInvalidContentType() throws Exception {
    networkNodes
        .addNode(Collections.singletonList(Box.KeyPair.random().publicKey()), new URL("http://127.0.0.1:9001/"));
    networkNodes
        .addNode(Collections.singletonList(Box.KeyPair.random().publicKey()), new URL("http://127.0.0.1:9002/"));

    // prepare /partyinfo payload (our known peers) with invalid content type (json)
    final RequestBody partyInfoBody =
        RequestBody.create(MediaType.parse(JSON.httpHeaderValue), Serializer.serialize(JSON, networkNodes));

    final Request request = new Request.Builder().post(partyInfoBody).url(nodeBaseUrl + "/partyinfo").build();

    final Response resp = httpClient.newCall(request).execute();
    assertEquals(404, resp.code());
  }

  @Test
  void partyInfoWithInvalidBody() throws Exception {
    final RequestBody partyInfoBody = RequestBody.create(MediaType.parse(CBOR.httpHeaderValue), "foo");

    final Request request = new Request.Builder().post(partyInfoBody).url(nodeBaseUrl + "/partyinfo").build();

    final Response resp = httpClient.newCall(request).execute();

    // produces 500 because serialisation error
    assertEquals(500, resp.code());
    // checks if the failure reason was with de-serialisation
    assertError(OrionErrorCode.OBJECT_JSON_DESERIALIZATION, resp);
  }
}
