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
package net.consensys.orion.exception;

import javax.annotation.Nullable;

import com.google.common.base.Objects;

/** Base exception class encapsulating the Orion error codes. */
public class OrionException extends RuntimeException {

  private final OrionErrorCode code;

  public OrionException(final OrionErrorCode code) {
    this.code = code;
  }

  public OrionException(final OrionErrorCode code, final String message) {
    this(code, message, null);
  }

  public OrionException(final OrionErrorCode code, @Nullable final Throwable cause) {
    super(cause);
    this.code = code;
  }

  public OrionException(final OrionErrorCode code, final String message, @Nullable final Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public OrionErrorCode code() {
    return code;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final OrionException that = (OrionException) o;
    return Objects.equal(this.code, that.code);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(code);
  }
}
