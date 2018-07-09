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

package net.consensys.orion.api.enclave;

import net.consensys.orion.api.exception.OrionErrorCode;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EncryptedPayload implements Serializable {

  private final byte[] combinedKeyNonce;
  private final PublicKey sender;
  private final byte[] cipherText;
  private final byte[] nonce;
  private final CombinedKey[] combinedKeys;
  private final Map<PublicKey, Integer> combinedKeysOwners;

  public EncryptedPayload(
      PublicKey sender,
      byte[] nonce,
      byte[] combinedKeyNonce,
      CombinedKey[] combinedKeys,
      byte[] cipherText) {
    this(sender, nonce, combinedKeyNonce, combinedKeys, cipherText, Collections.emptyMap());
  }

  @JsonCreator
  public EncryptedPayload(
      @JsonProperty("sender") PublicKey sender,
      @JsonProperty("nonce") byte[] nonce,
      @JsonProperty("combinedKeyNonce") byte[] combinedKeyNonce,
      @JsonProperty("combinedKeys") CombinedKey[] combinedKeys,
      @JsonProperty("cipherText") byte[] cipherText,
      @JsonProperty("combinedKeysOwners") Map<PublicKey, Integer> combinedKeysOwners) {
    this.combinedKeyNonce = combinedKeyNonce;
    this.sender = sender;
    this.cipherText = cipherText;
    this.nonce = nonce;
    this.combinedKeys = combinedKeys;
    this.combinedKeysOwners = combinedKeysOwners;
  }

  @JsonProperty("sender")
  public PublicKey sender() {
    return sender;
  }

  @JsonProperty("cipherText")
  public byte[] cipherText() {
    return cipherText;
  }

  @JsonProperty("nonce")
  public byte[] nonce() {
    return nonce;
  }

  @JsonProperty("combinedKeys")
  public CombinedKey[] combinedKeys() {
    return combinedKeys;
  }

  @JsonProperty("combinedKeyNonce")
  public byte[] combinedKeyNonce() {
    return combinedKeyNonce;
  }

  public EncryptedPayload stripFor(PublicKey key) {
    final Integer toKeepIdx = combinedKeysOwners.get(key);

    if (toKeepIdx == null || toKeepIdx < 0 || toKeepIdx >= combinedKeys.length) {
      throw new EnclaveException(
          OrionErrorCode.ENCLAVE_NOT_PAYLOAD_OWNER,
          "can't strip encrypted payload for provided key");
    }

    return new EncryptedPayload(
        sender,
        nonce,
        combinedKeyNonce,
        new CombinedKey[] {combinedKeys[toKeepIdx]},
        cipherText);
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
    return Arrays.equals(combinedKeyNonce, that.combinedKeyNonce)
        && Objects.equals(sender, that.sender)
        && Arrays.equals(cipherText, that.cipherText)
        && Arrays.equals(nonce, that.nonce)
        && Arrays.equals(combinedKeys, that.combinedKeys);
  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode(combinedKeyNonce);
    result = 31 * result + sender.hashCode();
    result = 31 * result + Arrays.hashCode(cipherText);
    result = 31 * result + Arrays.hashCode(nonce);
    result = 31 * result + Arrays.hashCode(combinedKeys);
    return result;
  }
}
