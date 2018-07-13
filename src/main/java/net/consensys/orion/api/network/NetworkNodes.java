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

package net.consensys.orion.api.network;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.orion.impl.network.ConcurrentNetworkNodes;

import java.net.URL;
import java.util.Collection;
import java.util.Map;

/** Details of other nodes on the network */
public interface NetworkNodes {

  /**
   * @return URL of node
   */
  URL url();

  /**
   * @return List of URLs of other nodes on the network
   */
  Collection<URL> nodeURLs();

  URL urlForRecipient(Box.PublicKey recipient);

  /**
   * @return Map from public key to node for all discovered nodes.
   */
  Map<Box.PublicKey, URL> nodePKs();

  boolean merge(ConcurrentNetworkNodes other);
}
