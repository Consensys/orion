/*
 * Copyright 2020 ConsenSys AG.
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

import static org.apache.tuweni.crypto.sodium.Box.KeyPair.random;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.orion.config.Config;

import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.tuweni.crypto.sodium.Box;
import org.apache.tuweni.kv.MapKeyValueStore;
import org.junit.jupiter.api.Test;

class PersistentNetworkNodesTest {

  @Test
  void testTwoInstancesSameStore() throws Exception {
    Config config = Config.load("tls='off'");
    MapKeyValueStore<Box.PublicKey, URI> store = MapKeyValueStore.open(new ConcurrentHashMap<>());
    PersistentNetworkNodes nodes1 = new PersistentNetworkNodes(config, new Box.PublicKey[0], store);
    PersistentNetworkNodes nodes2 = new PersistentNetworkNodes(config, new Box.PublicKey[0], store);

    Box.PublicKey pk = random().publicKey();
    boolean changed = nodes1.addNode(Collections.singletonMap(pk, URI.create("http://example:com:56666")).entrySet());
    assertTrue(changed);

    Iterator<Map.Entry<Box.PublicKey, URI>> iter = nodes2.nodePKs().iterator();
    assertTrue(iter.hasNext());
    Map.Entry<Box.PublicKey, URI> entry = iter.next();
    assertEquals(pk, entry.getKey());
    assertFalse(iter.hasNext());
  }

  @Test
  void testTwoInstancesInSequence() {
    Config config = Config.load("tls='off'");
    MapKeyValueStore<Box.PublicKey, URI> store = MapKeyValueStore.open(new ConcurrentHashMap<>());
    PersistentNetworkNodes nodes1 = new PersistentNetworkNodes(config, new Box.PublicKey[0], store);

    Box.PublicKey pk = random().publicKey();
    boolean changed = nodes1.addNode(Collections.singletonMap(pk, URI.create("http://example:com:56666")).entrySet());
    assertTrue(changed);

    PersistentNetworkNodes nodes2 = new PersistentNetworkNodes(config, new Box.PublicKey[0], store);
    Iterator<Map.Entry<Box.PublicKey, URI>> iter = nodes2.nodePKs().iterator();
    Map.Entry<Box.PublicKey, URI> entry = iter.next();
    assertEquals(pk, entry.getKey());
    assertFalse(iter.hasNext());
  }

  @Test
  void tryOverridingNodeInfo() {
    Config config = Config.load("tls='off'");
    MapKeyValueStore<Box.PublicKey, URI> store = MapKeyValueStore.open(new ConcurrentHashMap<>());
    PersistentNetworkNodes nodes = new PersistentNetworkNodes(config, new Box.PublicKey[0], store);
    Box.PublicKey pk = random().publicKey();
    boolean changed = nodes.addNode(Collections.singletonMap(pk, URI.create("http://example:com:56666")).entrySet());
    assertTrue(changed);

    changed = nodes.addNode(Collections.singletonMap(pk, URI.create("http://evil:com:56666")).entrySet());
    assertFalse(changed);

    assertEquals(URI.create("http://example:com:56666"), nodes.uriForRecipient(pk));

  }
}
