/*
 * Copyright 2019 ConsenSys AG.
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
package net.consensys.orion.http.handler.knownnodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import net.consensys.cava.crypto.sodium.Box.KeyPair;
import net.consensys.cava.crypto.sodium.Box.PublicKey;
import net.consensys.cava.io.Base64;
import net.consensys.orion.http.handler.HandlerTest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

class KnownNodesHandlerTest extends HandlerTest {

  @Test
  void shouldReturnListOfKnownNodesWhenOrionHasNodes() throws Exception {
    final List<KnownNode> nodes = createNodes();
    addNodesToNetwork(nodes);

    final Request request = new Request.Builder().get().url(clientBaseUrl + "/knownnodes").build();
    final Response response = httpClient.newCall(request).execute();
    final List<KnownNode> knownNodes = readList(response);

    assertEquals(nodes.size(), knownNodes.size());
    assertTrue(knownNodes.containsAll(nodes));
  }

  @Test
  void shouldReturnEmptyListWhenOrionHasNoNodes() throws Exception {
    final Request request = new Request.Builder().get().url(clientBaseUrl + "/knownnodes").build();
    final Response response = httpClient.newCall(request).execute();
    final List<KnownNode> knownNodes = readList(response);

    assertTrue(knownNodes.isEmpty());
  }

  @Test
  void knownNodesMethodIsAvailableOnClientApi() throws Exception {
    final Request request = new Request.Builder().get().url(clientBaseUrl + "/knownnodes").build();
    final Response response = httpClient.newCall(request).execute();

    assertEquals(200, response.code());
  }

  @Test
  void knownNodesMethodIsNotAvailableOnNodeApi() throws Exception {
    final Request request = new Request.Builder().get().url(nodeBaseUrl + "/knownnodes").build();
    final Response response = httpClient.newCall(request).execute();

    assertEquals(404, response.code());
  }

  private List<KnownNode> readList(final Response response) throws java.io.IOException {
    return new ObjectMapper().readValue(response.body().bytes(), new TypeReference<>() {});
  }

  private List<KnownNode> createNodes() {
    try {
      final List<KnownNode> knownNodes = new ArrayList<>();
      knownNodes.add(new KnownNode(KeyPair.random().publicKey(), new URL("http://127.0.0.1:9001/")));
      knownNodes.add(new KnownNode(KeyPair.random().publicKey(), new URL("http://127.0.0.1:9002/")));
      return knownNodes;
    } catch (Exception e) {
      fail(e);
      return new ArrayList<>();
    }
  }

  private void addNodesToNetwork(final Collection<KnownNode> nodes) {
    nodes.forEach(node -> {
      try {
        final PublicKey publicKey = PublicKey.fromBytes(Base64.decodeBytes(node.getPublicKey()));
        final URL nodeUrl = new URL(node.getNodeUrl());
        networkNodes.addNode(Collections.singletonList(publicKey), nodeUrl);
      } catch (MalformedURLException e) {
        fail(e);
      }
    });
  }

}
