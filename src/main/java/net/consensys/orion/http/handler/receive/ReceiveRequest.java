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

package net.consensys.orion.http.handler.receive;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReceiveRequest implements Serializable {
  public final String key;
  public final String to; // b64 encoded

  @JsonCreator
  public ReceiveRequest(@JsonProperty("key") String key, @JsonProperty("to") String to) {
    this.key = key;
    this.to = to;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ReceiveRequest)) {
      return false;
    }
    ReceiveRequest that = (ReceiveRequest) o;
    return Objects.equals(key, that.key) && Objects.equals(to, that.to);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, to);
  }

  @Override
  public String toString() {
    return "ReceiveRequest{" + "key='" + key + '\'' + ", to='" + to + '\'' + '}';
  }
}
