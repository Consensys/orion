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

public class PrivacyGroups implements Serializable {

  private String privacyGroupId;
  private boolean isDefault;

  public String getPrivacyGroupId() {
    return privacyGroupId;
  }

  public void setPrivacyGroupId(String privacyGroupId) {
    this.privacyGroupId = privacyGroupId;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public void setDefault(boolean aDefault) {
    isDefault = aDefault;
  }

  public PrivacyGroups() {}

  public PrivacyGroups(String privacyGroupId, boolean isDefault) {
    this.privacyGroupId = privacyGroupId;
    this.isDefault = isDefault;
  }
}
