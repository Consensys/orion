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

package net.consensys.orion.http.server;

import net.consensys.orion.exception.OrionErrorCode;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HttpError {

  private final OrionErrorCode error;

  public HttpError(OrionErrorCode error) {
    this.error = error;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HttpError httpError = (HttpError) o;
    return Objects.equals(error, httpError.error);
  }

  @Override
  public int hashCode() {
    return Objects.hash(error);
  }

  @Override
  public String toString() {
    return "HttpError{" + "error='" + error.code() + '\'' + '}';
  }

  @JsonProperty("error")
  public String error() {
    return error.code();
  }
}
