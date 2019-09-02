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
package net.consensys.orion.enclave;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

public class QueryPrivacyGroupPayload implements Serializable {

  private final String[] addresses;
  private final List<String> privacyGroupId;
  private String privacyGroupToModify = null;

  private boolean toDelete = false;

  @JsonCreator
  public QueryPrivacyGroupPayload(
      @JsonProperty("addresses") String[] addresses,
      @JsonProperty("privacyGroupId") List<String> privacyGroupId) {
    this.addresses = addresses;
    this.privacyGroupId = privacyGroupId;
  }

  @JsonProperty("addresses")
  public String[] addresses() {
    return addresses;
  }

  @JsonProperty("privacyGroupId")
  public List<String> privacyGroupId() {
    return privacyGroupId;
  }

  @JsonProperty("privacyGroupToModify")
  public String privacyGroupToModify() {
    return privacyGroupToModify;
  }

  @JsonSetter("privacyGroupToModify")
  public void setPrivacyGroupToModify(String privacyGroupToModify) {
    this.privacyGroupToModify = privacyGroupToModify;
  }


  public boolean isToDelete() {
    return toDelete;
  }

  public void setToDelete(boolean toDelete) {
    this.toDelete = toDelete;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QueryPrivacyGroupPayload that = (QueryPrivacyGroupPayload) o;
    return Arrays.equals(addresses, that.addresses)
        && Objects.equals(privacyGroupId, that.privacyGroupId)
        && Objects.equals(privacyGroupToModify, that.privacyGroupToModify);
  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode(addresses);
    result = 31 * result + privacyGroupId.hashCode();
    result = 31 * result + privacyGroupToModify.hashCode();
    return result;
  }
}
