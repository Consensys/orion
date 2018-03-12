package net.consensys.orion.impl.exception;

import java.util.Optional;

/** The set of error conditions that can be expected as output from an Orion. */
public enum OrionErrorCode {
  UNMAPPED("0x000000F"),

  /** 0x1xxxxx Node communication issues. */
  PAYLOAD_PROPAGATION_TO_ALL_PARTICIPANTS("0x100001"),

  /** 0x2xxxxx object mapping issues. */
  JSON_DESERIALIZATION("0x200001");

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
