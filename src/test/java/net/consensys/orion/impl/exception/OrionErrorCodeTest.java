package net.consensys.orion.impl.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.orion.api.exception.OrionErrorCode;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/** Verify the Orion Error Codes behave correctly. */
class OrionErrorCodeTest {

  @Test
  void propagationToAllParticipantsFailed() {
    final OrionErrorCode expected = OrionErrorCode.NODE_PROPAGATING_TO_ALL_PEERS;

    final Optional<OrionErrorCode> actual = OrionErrorCode.get(expected.code());

    assertTrue(actual.isPresent(), "Expecting an error code to be returned");
    assertEquals(expected, actual.get());
  }

  @Test
  void absent() {
    final String missingCode = "I don't really exist";

    final Optional<OrionErrorCode> actual = OrionErrorCode.get(missingCode);

    assertFalse(actual.isPresent(), "Expecting no error code to be returned");
  }
}
