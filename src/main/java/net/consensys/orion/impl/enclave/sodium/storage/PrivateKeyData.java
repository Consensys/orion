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

package net.consensys.orion.impl.enclave.sodium.storage;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PrivateKeyData {
  private final Optional<String> bytes;
  private final Optional<String> asalt;
  private final Optional<ArgonOptions> aopts;
  private final Optional<String> snonce;
  private final Optional<String> sbox;

  public PrivateKeyData(String bytes) {
    this(of(bytes), empty(), empty(), empty(), empty());
  }

  @JsonCreator
  public PrivateKeyData(
      @JsonProperty("bytes") Optional<String> bytes,
      @JsonProperty("asalt") Optional<String> asalt,
      @JsonProperty("aopts") Optional<ArgonOptions> aopts,
      @JsonProperty("snonce") Optional<String> snonce,
      @JsonProperty("sbox") Optional<String> sbox) {
    this.bytes = bytes;
    this.asalt = asalt;
    this.aopts = aopts;
    this.snonce = snonce;
    this.sbox = sbox;
  }

  /**
   * Base64 encoded bytes
   *
   * @return Base64 encoded bytes
   */
  @JsonProperty("bytes")
  public Optional<String> bytes() {
    return bytes;
  }

  /** @return The base64 encoded salt to use with Argon */
  @JsonProperty("asalt")
  public Optional<String> asalt() {
    return asalt;
  }

  /** @return General options to use with Argon */
  @JsonProperty("aopts")
  public Optional<ArgonOptions> aopts() {
    return aopts;
  }

  /**
   * @return base64 encoded nonce to use with the secret box encryption, using the password generated key.
   */
  @JsonProperty("snonce")
  public Optional<String> snonce() {
    return snonce;
  }

  /** @return base64 encoded Secret box containing the password protected private key. */
  @JsonProperty("sbox")
  public Optional<String> sbox() {
    return sbox;
  }

  public void aopts(ArgonOptions argonOptions) {}
}
