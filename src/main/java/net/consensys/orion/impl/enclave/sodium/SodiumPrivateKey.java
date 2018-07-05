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

import java.security.PrivateKey;
import java.util.Arrays;

public class SodiumPrivateKey implements PrivateKey {

  private final byte[] privateKey;

  public SodiumPrivateKey(byte[] privateKey) {
    this.privateKey = privateKey;
  }

  @Override
  public String getAlgorithm() {
    return "sodium";
  }

  @Override
  public String getFormat() {
    return "raw";
  }

  @Override
  public byte[] getEncoded() {
    return privateKey;
  }

  @Override
  public String toString() {
    return "SodiumPrivateKey{" + "privateKey=" + Arrays.toString(privateKey) + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SodiumPrivateKey that = (SodiumPrivateKey) o;

    return Arrays.equals(privateKey, that.privateKey);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(privateKey);
  }
}
