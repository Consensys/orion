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
package net.consensys.orion.http.handler.privacy;

import java.io.Serializable;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

public class PrivacyGroupRequest implements Serializable {

  private final String[] addresses;
  private final String from; // b64 encoded
  private final String name;
  private final String description;

  private byte[] seed;

  @JsonCreator
  public PrivacyGroupRequest(
      @JsonProperty("addresses") String[] addresses,
      @JsonProperty("from") String from,
      @JsonProperty("name") String name,
      @JsonProperty("description") String description) {
    this.addresses = addresses;
    this.from = from;
    this.name = name;
    this.description = description;
  }

  @JsonSetter
  public void setSeed(byte[] seed) {
    this.seed = seed;
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

  @JsonProperty("from")
  public String from() {
    return from;
  }

  public Optional<byte[]> getSeed() {
    return Optional.of(seed);
  }
}
