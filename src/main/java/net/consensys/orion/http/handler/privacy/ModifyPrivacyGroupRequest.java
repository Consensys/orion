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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ModifyPrivacyGroupRequest implements Serializable {

  private final String address;
  private final String from;
  private final String privacyGroupId;

  @JsonCreator
  public ModifyPrivacyGroupRequest(
      @JsonProperty("address") String address,
      @JsonProperty("from") String from,
      @JsonProperty("privacyGroupId") String privacyGroupId) {
    this.address = address;
    this.from = from;
    this.privacyGroupId = privacyGroupId;
  }

  @JsonProperty("address")
  public String address() {
    return address;
  }

  @JsonProperty("privacyGroupId")
  public String privacyGroupId() {
    return privacyGroupId;
  }

  @JsonProperty("from")
  public String from() {
    return from;
  }

}
