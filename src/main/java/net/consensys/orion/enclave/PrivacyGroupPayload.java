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
  private final String name;
  private final String description;
  private State state;
  private final Type type;
  private final byte[] randomSeed;

  @JsonCreator
  public PrivacyGroupPayload(
      @JsonProperty("addresses") final String[] addresses,
      @JsonProperty("name") final String name,
      @JsonProperty("description") final String description,
      @JsonProperty("state") final State state,
      @JsonProperty("type") final Type type,
      @JsonProperty("randomSeed") final byte[] randomSeed) {
    this.addresses = addresses;
    this.name = name;
    this.description = description;
    this.state = state;
    this.type = type;
    this.randomSeed = randomSeed;
  }

  @JsonProperty("addresses")
  public String[] addresses() {
    return addresses;
  }

  @JsonProperty("name")
  public String name() {
    return name;
  }

  @JsonProperty("description")
  public String description() {
    return description;
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

  public void setState(final State state) {
    this.state = state;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final PrivacyGroupPayload that = (PrivacyGroupPayload) o;
    return Arrays.equals(addresses, that.addresses)
        && Objects.equals(name, that.name)
        && Objects.equals(description, that.description)
        && Objects.equals(state, that.state)
        && Objects.equals(type, that.type)
        && Arrays.equals(randomSeed, that.randomSeed);
  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode(addresses);
    result = 31 * result + name.hashCode();
    result = 31 * result + description.hashCode();
    result = 31 * result + state.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + Arrays.hashCode(randomSeed);
    return result;
  }

  public enum State {
    ACTIVE, DELETED
  }
  public enum Type {
    LEGACY, PANTHEON
  }
}
