package net.consensys.orion.api.exception;

import java.util.Optional;

/**
 * The set of error conditions that can be expected as output from an Orion.
 *
 * <p>
 * The error codes are granular, ideally with each code mapping to a single permutation of each error scenario.
 */
public enum OrionErrorCode {
  UNMAPPED("0x000000F"),

  /** 0x1xxxxx Node communication issues. */
  NODE_MISSING_PEER_URL("NodeMissingPeerUrl"),
  NODE_PUSHING_TO_PEER("NodePushingToPeer"),
  NODE_PUSHING_TO_PEER_RESPONSE("NodePushingToPeerResponse"),
  NODE_PROPAGATING_TO_ALL_PEERS("NodePropagatingToAllPeers"),
  NO_SENDER_KEY("NoSenderKey"),
  INVALID_PAYLOAD("InvalidPayload"),

  /** 0x2xxxxx object mapping issues. */
  OBJECT_JSON_DESERIALIZATION("ObjectJsonDeserialization"),
  OBJECT_JSON_SERIALIZATION("ObjectJsonSerialization"),
  OBJECT_READ("ObjectRead"),
  OBJECT_UNSUPPORTED_TYPE("ObjectUnsupportedType"),
  OBJECT_WRITE("ObjectWrite"),

  /** 0x3xxxxx application configuration issues. */
  CONFIGURATION_OPTION("ConfigurationOption"),
  CONFIGURATION_STORAGE_MECHANISM("ConfigurationStorageMechanism"),

  /** 0x4xxxxx Enclave category of issues. */
  ENCLAVE_CREATE_KEY_PAIR("EnclaveCreateKeyPair"),
  ENCLAVE_DECODE_PUBLIC_KEY("EnclaveDecodePublicKey"),
  ENCLAVE_DECRYPT("EnclaveDecrypt"),
  ENCLAVE_DECRYPT_WRONG_PRIVATE_KEY("EnclaveDecryptWrongPrivateKey"),
  ENCLAVE_ENCRYPT("EnclaveEncrypt"),
  ENCLAVE_ENCRYPT_COMBINE_KEYS("EnclaveEncryptCombineKeys"),
  ENCLAVE_MISSING_PRIVATE_KEY_PASSWORD("EnclaveMissingPrivateKeyPasswords"),
  ENCLAVE_NO_MATCHING_PRIVATE_KEY("EnclaveNoMatchingPrivateKey"),
  ENCLAVE_NOT_PAYLOAD_OWNER("EnclaveNotPayloadOwner"),
  ENCLAVE_UNSUPPORTED_ALGORITHM("EnclaveUnsupportedAlgorithm"),
  ENCLAVE_UNSUPPORTED_PRIVATE_KEY_TYPE("EnclaveUnsupportedPrivateKeyType"),
  ENCLAVE_UNSUPPORTED_PUBLIC_KEY_TYPE("EnclaveUnsupportedPublicKeyType"),
  ENCLAVE_UNSUPPORTED_STORAGE_ALGORITHM("EnclaveUnsupportedStorageAlgorithm"),
  ENCLAVE_READ_PASSWORDS("EnclaveReadPasswords"),
  ENCLAVE_READ_PUBLIC_KEY("EnclaveReadPublicKeys"),
  ENCLAVE_STORAGE_ENCRYPT("EnclaveStorageEncrypt"),
  ENCLAVE_STORAGE_DECRYPT("EnclaveStorageDecrypt"),
  ENCLAVE_WRITE_PUBLIC_KEY("EnclaveWritePublicKey"),

  /** 0x5xxxxx Storage category of issues. */
  STORAGE_CLOSE("StorageClose"),
  STORAGE_CLOSED_DELETE("StorageClosedDelete"),
  STORAGE_CLOSED_READ("StorageClosedRead"),
  STORAGE_CLOSED_WRITE("StorageClosedWrite"),
  STORAGE_OPEN("StorageOpen"),

  SERVICE_START_ERROR("ServiceStartError"),
  SERVICE_START_INTERRUPTED("ServiceStartInterrupted"),
  CONFIG_FILE_MISSING("ConfigFileMissing");

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
