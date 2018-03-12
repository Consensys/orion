package net.consensys.orion.impl.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

/** Verify the Orion Error Codes behave correctly. */
public class OrionErrorCodeTest {

  @Test
  public void propagationToAllParticipantsFailed() {
    final OrionErrorCode expected = OrionErrorCode.PAYLOAD_PROPAGATION_TO_ALL_PARTICIPANTS;

    final Optional<OrionErrorCode> actual = OrionErrorCode.get(expected.code());

    assertTrue("Expecting an error code to be returned", actual.isPresent());
    assertEquals(expected, actual.get());
  }

  @Test
  public void absent() {
    final Byte missingCode = Byte.decode("0x000000");

    final Optional<OrionErrorCode> actual = OrionErrorCode.get(missingCode);

    assertFalse("Expecting no error code to be returned", actual.isPresent());
  }
}
