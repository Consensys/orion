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

import net.consensys.orion.enclave.sodium.serialization.PublicKeyMapKeyDeserializer;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.tuweni.bytes.Bytes;

public class ReadOnlyNetworkNodes implements NetworkNodes {

  private final URI uri;
  private final Map<Bytes, URI> nodePKs;

  @JsonCreator
  public ReadOnlyNetworkNodes(
      @JsonProperty("url") final URI uri,
      @JsonProperty("nodeURLs") List<URI> nodeURIs,
      @JsonProperty("nodePKs") @JsonDeserialize(
          keyUsing = PublicKeyMapKeyDeserializer.class) final Map<Bytes, URI> nodePKs) {
    this.uri = uri;
    this.nodePKs = new HashMap<>(nodePKs);
  }

  public ReadOnlyNetworkNodes(URI uri, Map<Bytes, URI> nodePKs) {
    this(uri, Collections.emptyList(), nodePKs);
  }

  @Override
  public URI uri() {
    return uri;
  }

  @Override
  public Collection<URI> nodeURIs() {
    return new ArrayList<>(nodePKs.values());
  }

  @Override
  public Iterable<Map.Entry<Bytes, URI>> nodePKs() {
    return nodePKs.entrySet();
  }

  @Override
  public URI uriForRecipient(final Bytes recipient) {
    return nodePKs.get(recipient);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ReadOnlyNetworkNodes that = (ReadOnlyNetworkNodes) o;
    return uri.equals(that.uri) && nodePKs.equals(that.nodePKs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uri, nodePKs);
  }
}
