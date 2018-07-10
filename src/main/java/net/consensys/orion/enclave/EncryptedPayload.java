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

package net.consensys.orion.enclave;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.orion.enclave.sodium.serialization.PublicKeyDeserializer;
import net.consensys.orion.enclave.sodium.serialization.PublicKeyMapKeyDeserializer;
import net.consensys.orion.enclave.sodium.serialization.PublicKeySerializer;
import net.consensys.orion.exception.OrionErrorCode;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class EncryptedPayload implements Serializable {

  private final Box.PublicKey sender;
  private final byte[] nonce;
  private final EncryptedKey[] encryptedKeys;
  private final byte[] cipherText;
  private final Map<Box.PublicKey, Integer> encryptedKeyOwners;

  public EncryptedPayload(Box.PublicKey sender, byte[] nonce, EncryptedKey[] encryptedKeys, byte[] cipherText) {
    this(sender, nonce, encryptedKeys, cipherText, Collections.emptyMap());
  }

  @JsonCreator
  public EncryptedPayload(
      @JsonProperty("sender") @JsonDeserialize(using = PublicKeyDeserializer.class) Box.PublicKey sender,
      @JsonProperty("nonce") byte[] nonce,
      @JsonProperty("encryptedKeys") EncryptedKey[] encryptedKeys,
      @JsonProperty("cipherText") byte[] cipherText,
      @JsonProperty("encryptedKeyOwners") @JsonDeserialize(
          keyUsing = PublicKeyMapKeyDeserializer.class) Map<Box.PublicKey, Integer> encryptedKeyOwners) {
    this.sender = sender;
    this.nonce = nonce;
    this.encryptedKeys = encryptedKeys;
    this.cipherText = cipherText;
    this.encryptedKeyOwners = encryptedKeyOwners;
  }

  @JsonProperty("sender")
  @JsonSerialize(using = PublicKeySerializer.class)
  public Box.PublicKey sender() {
    return sender;
  }

  @JsonProperty("cipherText")
  public byte[] cipherText() {
    return cipherText;
  }

  @JsonProperty("encryptedKeys")
  public EncryptedKey[] encryptedKeys() {
    return encryptedKeys;
  }

  @JsonProperty("nonce")
  public byte[] nonce() {
    return nonce;
  }

  public EncryptedPayload stripFor(Box.PublicKey key) {
    final Integer toKeepIdx = encryptedKeyOwners.get(key);

    if (toKeepIdx == null || toKeepIdx < 0 || toKeepIdx >= encryptedKeys.length) {
      throw new EnclaveException(
          OrionErrorCode.ENCLAVE_NOT_PAYLOAD_OWNER,
          "can't strip encrypted payload for provided key");
    }

    return new EncryptedPayload(sender, nonce, new EncryptedKey[] {encryptedKeys[toKeepIdx]}, cipherText);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EncryptedPayload that = (EncryptedPayload) o;
    return Objects.equals(sender, that.sender)
        && Arrays.equals(nonce, that.nonce)
        && Arrays.equals(encryptedKeys, that.encryptedKeys)
        && Arrays.equals(cipherText, that.cipherText);
  }

  @Override
  public int hashCode() {
    int result = sender.hashCode();
    result = 31 * result + Arrays.hashCode(nonce);
    result = 31 * result + Arrays.hashCode(encryptedKeys);
    result = 31 * result + Arrays.hashCode(cipherText);
    return result;
  }
}
