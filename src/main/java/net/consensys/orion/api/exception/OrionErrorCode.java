package net.consensys.orion.api.exception;

import java.util.Optional;

/**
 * The set of error conditions that can be expected as output from an Orion.
 *
 * <p>The error codes are granular, ideally with each code mapping to a single permutation of each
 * error scenario.
 */
public enum OrionErrorCode {
  UNMAPPED("0x000000F"),

  /** 0x1xxxxx Node communication issues. */
  NODE_MISSING_PEER_URL("0x100000"),
  NODE_PUSHING_TO_PEER("0x100001"),
  NODE_PUSHING_TO_PEER_RESPONSE("0x100002"),
  NODE_PROPAGATION_TO_ALL_PEERS("0x100003"),

  /** 0x2xxxxx object mapping issues. */
  OBJECT_JSON_DESERIALIZATION("0x200000"),
  OBJECT_JSON_SERIALIZATION("0x200001"),
  OBJECT_READ("0x200002"),
  OBJECT_UNSUPPORTED_TYPE("0x200003"),
  OBJECT_WRITE("0x200004"),

  /** 0x3xxxxx application configuration issues. */
  CONFIGURATION_OPTION("0x300000"),
  CONFIGURATION_STORAGE_MECHANISM("0x300001"),

  /** 0x4xxxxx Encalve category of issues. */
  ENCLAVE_CREATE_KEY_PAIR("0x040000"),
  ENCLAVE_DECODE_PUBLIC_KEY("0x040001"),
  ENCLAVE_DECRYPT("0x040002"),
  ENCLAVE_DECRYPT_WRONG_PRIVATE_KEY("0x040003"),
  ENCLAVE_ENCRYPT("0x040004"),
  ENCLAVE_ENCRYPT_COMBINE_KEYS("0x040005"),
  ENCLAVE_MISSING_PRIVATE_KEY_PASSWORD("0x040006"),
  ENCLAVE_NO_MATCHING_PRIVATE_KEY("0x040007"),
  ENCLAVE_NOT_PAYLOAD_OWNER("0x040008"),
  ENCLAVE_UNSUPPORTED_ALGORITHM("0x040009"),
  ENCLAVE_UNSUPPORTED_PRIVATE_KEY_TYPE("0x04000A"),
  ENCLAVE_UNSUPPORTED_PUBLIC_KEY_TYPE("0x04000B"),
  ENCLAVE_UNSUPPORTED_STORAGE_ALGORITHM("0x04000C"),
  ENCLAVE_READ_PASSWORDS("0x04000D"),
  ENCLAVE_READ_PUBLIC_KEY("0x04000E"),
  ENCLAVE_STORAGE_ENCRYPT("0x04000F"),
  ENCLAVE_STORAGE_DECRYPT("0x040010"),
  ENCLAVE_WRITE_PUBLIC_KEY("0x040011"),

  /** 0x5xxxxx Storage category of issues. */
  STORAGE_CLOSE("0x050000"),
  STORAGE_CLOSED_DELETE("0x050001"),
  STORAGE_CLOSED_READ("0x050002"),
  STORAGE_CLOSED_WRITE("0x050003"),
  STORAGE_OPEN("0x050004");

  /** Integer supports 0x000001 to 0xFFFFFF (1 to 16,777,215). */
  private final int code;

  OrionErrorCode(final String code) {
    this.code = Integer.decode(code);
  }

  public int code() {
    return code;
  }

  public static Optional<OrionErrorCode> get(final int code) {

    for (final OrionErrorCode candidate : OrionErrorCode.values()) {
      if (candidate.code == code) {
        return Optional.of(candidate);
      }
    }

    return Optional.empty();
  }
}
