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
package net.consensys.orion.http.handler.tx;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request to add a commitment for an enclaveKey to Orion storage
 */
public class PushToHistoryRequest implements Serializable {
  private final String privacyGroupId;
  private final String privacyMarkerTxHash;
  private final String enclaveKey;

  @JsonCreator
  public PushToHistoryRequest(
      @JsonProperty("privacyGroupId") String privacyGroupId,
      @JsonProperty("privacyMarkerTransactionHash") String privacyMarkerTransactionHash,
      @JsonProperty("enclaveKey") String enclaveKey) {

    this.privacyGroupId = privacyGroupId;
    this.privacyMarkerTxHash = privacyMarkerTransactionHash;
    this.enclaveKey = enclaveKey;
  }

  @JsonProperty("privacyGroupId")
  public String privacyGroupId() {
    return privacyGroupId;
  }

  @JsonProperty("privacyMarkerTransactionHash")
  public String privacyMarkerTxHash() {
    return privacyMarkerTxHash;
  }

  @JsonProperty("enclaveKey")
  public String enclaveKey() {
    return enclaveKey;
  }
}
