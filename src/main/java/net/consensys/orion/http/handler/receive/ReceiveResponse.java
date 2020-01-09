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
package net.consensys.orion.http.handler.receive;

import java.io.Serializable;

public class ReceiveResponse implements Serializable {
  byte[] payload;
  byte[] privacyGroupId;
  byte[] senderKey;

  public ReceiveResponse() {}

  public ReceiveResponse(final byte[] payload, final byte[] privacyGroupId, final byte[] senderKey) {
    this.payload = payload;
    this.privacyGroupId = privacyGroupId;
    this.senderKey = senderKey;
  }

  public byte[] getPayload() {
    return payload;
  }

  public void setPayload(final byte[] payload) {
    this.payload = payload;
  }

  public byte[] getPrivacyGroupId() {
    return privacyGroupId;
  }

  public void setPrivacyGroupId(final byte[] privacyGroupId) {
    this.privacyGroupId = privacyGroupId;
  }

  public byte[] getSenderKey() {
    return senderKey;
  }

  public void setSenderKey(final byte[] senderKey) {
    this.senderKey = senderKey;
  }
}
