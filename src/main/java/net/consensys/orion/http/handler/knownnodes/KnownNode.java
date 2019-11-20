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

import net.consensys.cava.crypto.sodium.Box.PublicKey;

import java.net.URL;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.postgresql.util.Base64;

class KnownNode {

  private final String publicKey;
  private final String nodeUrl;

  KnownNode(final PublicKey publicKey, final URL nodeUrl) {
    this.publicKey = Base64.encodeBytes(publicKey.bytesArray());
    this.nodeUrl = nodeUrl.toString();
  }

  @JsonCreator
  KnownNode(@JsonProperty("publicKey") final String publicKey, final @JsonProperty("nodeUrl") String nodeUrl) {
    this.publicKey = publicKey;
    this.nodeUrl = nodeUrl;
  }

  @JsonProperty("publicKey")
  String getPublicKey() {
    return publicKey;
  }

  @JsonProperty("nodeUrl")
  String getNodeUrl() {
    return nodeUrl;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final KnownNode knownNode = (KnownNode) o;
    return Objects.equal(publicKey, knownNode.publicKey) && Objects.equal(nodeUrl, knownNode.nodeUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(publicKey, nodeUrl);
  }
}
