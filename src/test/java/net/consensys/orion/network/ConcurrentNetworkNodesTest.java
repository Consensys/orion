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
package net.consensys.orion.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.utils.Serializer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.tuweni.crypto.sodium.Box;
import org.junit.jupiter.api.Test;

class ConcurrentNetworkNodesTest {

  @Test
  void roundTripSerialization() throws MalformedURLException {
    final URL u = new URL("http://nowhere:9090/");
    final List<URL> urls = Collections.singletonList(u);
    final Map<Box.PublicKey, URL> pks = Collections.singletonMap(Box.KeyPair.random().publicKey(), u);
    final ConcurrentNetworkNodes nodes = new ConcurrentNetworkNodes(new URL("http://some.server:8080/"), urls, pks);
    byte[] bytes = Serializer.serialize(HttpContentType.JSON, nodes);
    assertEquals(nodes, Serializer.deserialize(HttpContentType.JSON, ConcurrentNetworkNodes.class, bytes));
    bytes = Serializer.serialize(HttpContentType.CBOR, nodes);
    assertEquals(nodes, Serializer.deserialize(HttpContentType.CBOR, ConcurrentNetworkNodes.class, bytes));
  }
}
