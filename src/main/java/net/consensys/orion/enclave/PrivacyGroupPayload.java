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
package net.consensys.orion.enclave;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PrivacyGroupPayload implements Serializable {

  private final String[] addresses;
  private State state;
  private final Type type;
  private final byte[] randomSeed;

  @JsonCreator
  public PrivacyGroupPayload(
      @JsonProperty("addresses") String[] addresses,
      @JsonProperty("state") State state,
      @JsonProperty("type") Type type,
      @JsonProperty("randomSeed") byte[] randomSeed) {
    this.addresses = addresses;
    this.state = state;
    this.type = type;
    this.randomSeed = randomSeed;
  }

  @JsonProperty("addresses")
  public String[] addresses() {
    return addresses;
  }

  @JsonProperty("state")
  public State state() {
    return state;
  }

  @JsonProperty("type")
  public Type type() {
    return type;
  }

  @JsonProperty("randomSeed")
  public byte[] randomSeed() {
    return randomSeed;
  }

  public void setState(State state) {
    this.state = state;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrivacyGroupPayload that = (PrivacyGroupPayload) o;
    return Arrays.equals(addresses, that.addresses)
        && Objects.equals(state, that.state)
        && Objects.equals(type, that.type)
        && Arrays.equals(randomSeed, that.randomSeed);
  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode(addresses);
    result = 31 * result + state.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + Arrays.hashCode(randomSeed);
    return result;
  }

  public enum State {
    ACTIVE, DELETED
  }
  public enum Type {
    QUORUM, PANTHEON
  }
}
