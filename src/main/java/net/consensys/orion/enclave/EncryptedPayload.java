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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
  private final byte[] privacyGroupId;

  public EncryptedPayload(
      final Box.PublicKey sender,
      final byte[] nonce,
      final EncryptedKey[] encryptedKeys,
      final byte[] cipherText,
      final byte[] privacyGroupId) {
    this(sender, nonce, encryptedKeys, cipherText, Collections.emptyMap(), privacyGroupId);
  }

  @JsonCreator
  public EncryptedPayload(
      @JsonProperty("sender") @JsonDeserialize(using = PublicKeyDeserializer.class) final Box.PublicKey sender,
      @JsonProperty("nonce") final byte[] nonce,
      @JsonProperty("encryptedKeys") final EncryptedKey[] encryptedKeys,
      @JsonProperty("cipherText") final byte[] cipherText,
      @JsonProperty("encryptedKeyOwners") @JsonDeserialize(
          keyUsing = PublicKeyMapKeyDeserializer.class) final Map<Box.PublicKey, Integer> encryptedKeyOwners,
      @JsonProperty("privacyGroupId") final byte[] privacyGroupId) {
    this.sender = sender;
    this.nonce = nonce;
    this.encryptedKeys = encryptedKeys;
    this.cipherText = cipherText;
    this.encryptedKeyOwners = encryptedKeyOwners;
    this.privacyGroupId = privacyGroupId;
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

  @JsonProperty("privacyGroupId")
  public byte[] privacyGroupId() {
    return privacyGroupId;
  }

  public EncryptedPayload stripFor(final List<Box.PublicKey> keys) {
    final List<EncryptedKey> keepKeys =
        keys.stream().map(key -> encryptedKeys[encryptedKeyOwners.get(key)]).filter(Objects::nonNull).collect(
            Collectors.toList());

    if (keepKeys.size() != keys.size()) {
      throw new EnclaveException(
          OrionErrorCode.ENCLAVE_NOT_PAYLOAD_OWNER,
          "can't strip encrypted payload for provided key");
    }
    return new EncryptedPayload(
        sender,
        nonce,
        keepKeys.toArray(new EncryptedKey[keepKeys.size()]),
        cipherText,
        privacyGroupId);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final EncryptedPayload that = (EncryptedPayload) o;
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
