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

import net.consensys.orion.enclave.sodium.serialization.PublicKeyURISerializer;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.tuweni.crypto.sodium.Box;

/** Details of other nodes on the network */
public interface NetworkNodes {

  /**
   * @return URL of node
   */
  @JsonProperty("uri")
  URI uri();

  /**
   * @return List of URIs of other nodes on the network
   */
  @JsonProperty("nodeURIs")
  Collection<URI> nodeURIs();

  /**
   * Provide the URI associated with a public key.
   * 
   * @param recipient the public key of the recipient of a message
   * @return the URI, or null if no recipient exists for the given URI.
   */
  URI uriForRecipient(Box.PublicKey recipient);

  /**
   * @return Map from public key to node for all discovered nodes.
   */
  @JsonProperty("nodePKs")
  @JsonSerialize(using = PublicKeyURISerializer.class)
  Iterable<Map.Entry<Box.PublicKey, URI>> nodePKs();
}
