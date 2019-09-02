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

import net.consensys.orion.enclave.PrivacyGroupPayload;

import java.io.Serializable;
import java.util.Arrays;

public class PrivacyGroup implements Serializable {

  private String privacyGroupId;
  private String name;
  private String description;
  private PrivacyGroupPayload.Type type;
  private String[] members;

  public String getPrivacyGroupId() {
    return privacyGroupId;
  }

  public void setPrivacyGroupId(final String privacyGroupId) {
    this.privacyGroupId = privacyGroupId;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public PrivacyGroupPayload.Type getType() {
    return type;
  }

  public void setType(final PrivacyGroupPayload.Type type) {
    this.type = type;
  }

  public String[] getMembers() {
    return members;
  }

  public void setMembers(final String[] members) {
    this.members = members;
  }

  public PrivacyGroup() {}



  public PrivacyGroup(
      final String privacyGroupId,
      final PrivacyGroupPayload.Type type,
      final String name,
      final String description,
      final String[] members) {
    this.privacyGroupId = privacyGroupId;
    this.type = type;
    this.name = name;
    this.description = description;
    this.members = members;
  }

  @Override
  public String toString() {
    return "PrivacyGroup{"
        + "privacyGroupId='"
        + privacyGroupId
        + '\''
        + ", name='"
        + name
        + '\''
        + ", description='"
        + description
        + '\''
        + ", type="
        + type
        + ", members="
        + Arrays.toString(members)
        + '}';
  }
}
