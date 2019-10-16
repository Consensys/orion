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
package net.consensys.orion.http.handler.send;

import static net.consensys.cava.io.Base64.decodeBytes;
import static net.consensys.cava.io.Base64.encodeBytes;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.base.Strings;

public class SendRequest implements Serializable {

  @JsonProperty("from")
  private final String from; // b64 encoded
  @JsonProperty("to")
  private String[] to; // b64 encoded
  @JsonProperty("payload")
  private final byte[] rawPayload;
  private String privacyGroupId = null;

  public Optional<String> privacyGroupId() {
    return Optional.ofNullable(privacyGroupId);
  }

  public Optional<String> from() {
    return Optional.ofNullable(from);
  }

  public String[] to() {
    return to;
  }

  public String payload() {
    if (rawPayload == null) {
      return null;
    }
    return encodeBytes(rawPayload);
  }

  @JsonCreator
  public SendRequest(
      @JsonProperty("payload") final String payload,
      @JsonProperty("from") final String from,
      @JsonProperty("to") final String[] to) {
    this(decodePayload(payload), from, to);
  }

  @JsonSetter("privacyGroupId")
  public void setPrivacyGroupId(final String privacyGroupId) {
    this.privacyGroupId = privacyGroupId;
  }

  private static byte[] decodePayload(final String payload) {
    if (payload == null) {
      return new byte[0];
    }
    try {
      return decodeBytes(payload);
    } catch (final IllegalArgumentException e) {
      return null;
    }
  }

  public SendRequest(final byte[] rawPayload, final String from, final String[] to) {
    this.rawPayload = rawPayload;
    this.from = from;
    this.to = to;

    if (from != null && (to == null || to.length == 0)) {
      this.to = new String[] {from().orElse(null)};
    }
  }

  @JsonIgnore
  public boolean isValid() {
    return ((to != null && to.length > 0 && Arrays.stream(to).noneMatch(Strings::isNullOrEmpty))
        || (privacyGroupId != null && privacyGroupId.length() > 0))
        && rawPayload != null
        && rawPayload.length > 0
        && (from == null || from.length() > 0);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SendRequest)) {
      return false;
    }
    final SendRequest that = (SendRequest) o;
    return Arrays.equals(rawPayload, that.rawPayload) && Objects.equals(from, that.from) && Arrays.equals(to, that.to);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, Arrays.hashCode(to), Arrays.hashCode(rawPayload));
  }

  @Override
  public String toString() {
    return "SendRequest{"
        + "payload='"
        + encodeBytes(rawPayload)
        + '\''
        + ", from='"
        + from
        + '\''
        + ", to="
        + Arrays.toString(to)
        + '}';
  }

  public byte[] rawPayload() {
    return rawPayload;
  }
}
