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

import com.fasterxml.jackson.annotation.JsonProperty;

public class TransactionPair {

  private final String txPointer;
  private final String value;


  public TransactionPair(@JsonProperty("txPointer") String txPointer, @JsonProperty("value") String value) {
    this.txPointer = txPointer;
    this.value = value;
  }

  @JsonProperty("value")
  public String value() {
    return value;
  }

  @JsonProperty("txPointer")
  public String txPointer() {
    return txPointer;
  }
}
