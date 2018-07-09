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

package net.consensys.orion.impl.enclave.sodium;

import net.consensys.orion.impl.utils.Base64;

import java.security.PublicKey;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SodiumPublicKey implements PublicKey {

  private final byte[] encoded;

  public SodiumPublicKey(String base64) {
    this.encoded = Base64.decode(base64);
  }

  @JsonCreator
  public SodiumPublicKey(@JsonProperty("encoded") byte[] encoded) {
    this.encoded = encoded;
  }

  @Override
  public String getAlgorithm() {
    return null;
  }

  @Override
  public String getFormat() {
    return null;
  }

  @Override
  public byte[] getEncoded() {
    return encoded;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SodiumPublicKey publicKey1 = (SodiumPublicKey) o;

    return Arrays.equals(encoded, publicKey1.encoded);
  }

  @Override
  public String toString() {
    return Base64.encode(encoded);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(encoded);
  }
}
