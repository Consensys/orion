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

import java.util.Optional;

/**
 * The set of error conditions that can be expected as output from an Orion.
 *
 * <p>
 * The error codes are granular, ideally with each code mapping to a single permutation of each error scenario.
 */
public enum OrionErrorCode {
  UNMAPPED("0x000000F"),

  /** Node communication issues. */
  NODE_MISSING_PEER_URL("NodeMissingPeerUrl"),
  NODE_PUSHING_TO_PEER("NodePushingToPeer"),
  NODE_PROPAGATING_TO_ALL_PEERS("NodePropagatingToAllPeers"),
  NO_SENDER_KEY("NoSenderKey"),
  INVALID_PAYLOAD("InvalidPayload"),

  /** Object mapping issues. */
  OBJECT_JSON_DESERIALIZATION("ObjectJsonDeserialization"),
  OBJECT_JSON_SERIALIZATION("ObjectJsonSerialization"),
  OBJECT_UNSUPPORTED_TYPE("ObjectUnsupportedType"),

  /** Enclave category of issues. */
  ENCLAVE_CREATE_KEY_PAIR("EnclaveCreateKeyPair"),
  ENCLAVE_DECODE_PUBLIC_KEY("EnclaveDecodePublicKey"),
  ENCLAVE_DECRYPT_WRONG_PRIVATE_KEY("EnclaveDecryptWrongPrivateKey"),
  ENCLAVE_ENCRYPT_COMBINE_KEYS("EnclaveEncryptCombineKeys"),
  ENCLAVE_MISSING_PRIVATE_KEY_PASSWORD("EnclaveMissingPrivateKeyPasswords"),
  ENCLAVE_NO_MATCHING_PRIVATE_KEY("EnclaveNoMatchingPrivateKey"),
  ENCLAVE_NOT_PAYLOAD_OWNER("EnclaveNotPayloadOwner"),
  ENCLAVE_UNSUPPORTED_PRIVATE_KEY_TYPE("EnclaveUnsupportedPrivateKeyType"),
  ENCLAVE_STORAGE_DECRYPT("EnclaveStorageDecrypt"),
  ENCLAVE_PRIVACY_GROUP_CREATION("EnclavePrivacyGroupIdCreation"),

  /** Storing privacy group issue */
  ENCLAVE_UNABLE_STORE_PRIVACY_GROUP("PrivacyGroupNotStored"),
  ENCLAVE_PRIVACY_GROUP_MISSING("PrivacyGroupNotFound");

  private final String code;

  OrionErrorCode(final String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }

  public static Optional<OrionErrorCode> get(final String code) {

    for (final OrionErrorCode candidate : OrionErrorCode.values()) {
      if (candidate.code.equals(code)) {
        return Optional.of(candidate);
      }
    }

    return Optional.empty();
  }
}
