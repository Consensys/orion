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

import net.consensys.orion.config.Config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.crypto.sodium.Box;
import org.apache.tuweni.kv.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistentNetworkNodes implements NetworkNodes {
  private final static Logger logger = LoggerFactory.getLogger(PersistentNetworkNodes.class);
  private URI uri;
  private final KeyValueStore<Box.PublicKey, URI> nodePKs;

  public PersistentNetworkNodes(
      final Config config,
      final Box.PublicKey[] publicKeys,
      final KeyValueStore<Box.PublicKey, URI> store) {
    nodePKs = store;
    config.nodeUrl().ifPresent(nodeURL -> {
      try {
        setNodeUrl(nodeURL.toURI(), publicKeys);
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException(e);
      }
    });
  }

  /**
   * Set the url of the node we are running on. This is useful to do when we are running using default ports, and the
   * construction of this class will be performed before we have settled on what port Orion will be running on.
   *
   * @param uri URI of the node.
   * @param publicKeys PublicKeys used by the node.
   */
  public void setNodeUrl(final URI uri, final Box.PublicKey[] publicKeys) {
    this.uri = uri;
    try {
      AsyncCompletion.allOf(Arrays.stream(publicKeys).map(pk -> nodePKs.putAsync(pk, uri))).join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Add a node's URL and public keys to the nodeURLs and nodePKs lists
   *
   * @param nodesPks List of PublicKeys of new node
   */
  public boolean addNode(final Iterable<Map.Entry<Box.PublicKey, URI>> nodesPks) {
    logger.trace("addNode called");
    AtomicBoolean changed = new AtomicBoolean(false);
    List<AsyncCompletion> completions = new ArrayList<>();
    nodesPks.forEach(entry -> {
      Box.PublicKey nodePk = entry.getKey();
      URI nodeURI = entry.getValue();
      completions.add(nodePKs.getAsync(nodePk).thenAccept(oldNodeURL -> {
        if (oldNodeURL == null) {
          nodePKs.putAsync(nodePk, nodeURI);
          changed.set(true);
        }
      }));
    });
    try {
      AsyncCompletion.allOf(completions).join();
    } catch (InterruptedException e) {
      logger.warn("Timeout waiting to store node URL", e);
    }
    return changed.get();
  }

  @Override
  public URI uri() {
    return uri;
  }

  @Override
  public Collection<URI> nodeURIs() {
    Set<URI> allURIs = new HashSet<>();
    List<AsyncCompletion> completions = new ArrayList<>();
    nodePKs.keysAsync().thenAccept(keys -> {
      for (Box.PublicKey key : keys) {
        completions.add(nodePKs.getAsync(key).thenAccept(allURIs::add));
      }
    });
    try {
      AsyncCompletion.allOf(completions).join();
    } catch (InterruptedException e) {
      logger.warn("Timeout waiting to collect all URLs", e);
    }
    return allURIs;
  }

  @Override
  public URI uriForRecipient(final Box.PublicKey recipient) {
    try {
      return nodePKs.getAsync(recipient).get();
    } catch (InterruptedException e) {
      logger.warn("Timeout waiting to retrieve URL information", e);
      return null;
    }
  }

  @Override
  public Iterable<Map.Entry<Box.PublicKey, URI>> nodePKs() {
    try {
      Iterator<Box.PublicKey> iter = nodePKs.keysAsync().get().iterator();
      return () -> new Iterator<>() {
        @Override
        public boolean hasNext() {
          return iter.hasNext();
        }

        @Override
        public Map.Entry<Box.PublicKey, URI> next() {
          Box.PublicKey key = iter.next();
          return new Map.Entry<>() {

            @Override
            public Box.PublicKey getKey() {
              return key;
            }

            @Override
            public URI getValue() {
              return uriForRecipient(key);
            }

            @Override
            public URI setValue(URI value) {
              throw new UnsupportedOperationException();
            }
          };
        }
      };
    } catch (InterruptedException e) {
      logger.warn("Timeout waiting to retrieve keys", e);
      return null;
    }
  }

  /**
   * Merge with another set of node information.
   * 
   * @param other the other set of nodes
   * @return true if we updated our records.
   */
  public boolean merge(final NetworkNodes other) {
    return addNode(other.nodePKs());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof PersistentNetworkNodes))
      return false;
    PersistentNetworkNodes that = (PersistentNetworkNodes) o;
    return Objects.equals(uri, that.uri) && Objects.equals(nodePKs, that.nodePKs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uri, nodePKs);
  }
}
