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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.consensys.orion.enclave.TransactionPair;

import java.io.Serializable;

/**
 * Find the privacy group given the privacyGroupId.
 */
public class TxPrivacyGroupRequest implements Serializable {
  private TransactionPair payload;
  private String privacyGroupId;

  @JsonCreator
  public TxPrivacyGroupRequest(
      @JsonProperty("transactionPair") TransactionPair payload,
      @JsonProperty("privacyGroupId") String privacyGroupId) {
    this.payload = payload;
    this.privacyGroupId = privacyGroupId;
  }

  @JsonProperty("transactionPair")
  public TransactionPair payload() {
    return payload;
  }

  @JsonProperty("privacyGroupId")
  public String privacyGroupId() {
    return privacyGroupId;
  }
}
