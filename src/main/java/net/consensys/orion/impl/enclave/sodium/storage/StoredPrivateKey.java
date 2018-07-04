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

package net.consensys.orion.impl.enclave.sodium.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class StoredPrivateKey {
  public static final String UNLOCKED = "unlocked";
  public static final String ARGON2_SBOX = "sodiumargon2sbox";

  private final PrivateKeyData data;
  private final String type;

  @JsonCreator
  public StoredPrivateKey(@JsonProperty("data") PrivateKeyData data, @JsonProperty("type") String type) {
    this.data = data;
    this.type = type;
  }

  @JsonProperty("data")
  public PrivateKeyData data() {
    return data;
  }

  @JsonProperty("type")
  public String type() {
    return type;
  }
}
