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

package net.consensys.orion.api.enclave;

import net.consensys.orion.impl.utils.Base64;

import java.util.Arrays;

public class PrivateKey {

  private final byte[] keyBytes;

  public PrivateKey(byte[] keyBytes) {
    this.keyBytes = keyBytes;
  }

  public byte[] toBytes() {
    return keyBytes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PrivateKey that = (PrivateKey) o;

    return Arrays.equals(keyBytes, that.keyBytes);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(keyBytes);
  }

  @Override
  public String toString() {
    return "PrivateKey{" + Base64.encode(keyBytes) + '}';
  }
}
