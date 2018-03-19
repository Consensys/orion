package net.consensys.orion.impl.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.consensys.orion.api.exception.OrionErrorCode;

import java.util.Optional;

import org.junit.Test;

/** Verify the Orion Error Codes behave correctly. */
public class OrionErrorCodeTest {

  @Test
  public void propagationToAllParticipantsFailed() {
    final OrionErrorCode expected = OrionErrorCode.NODE_PROPAGATING_TO_ALL_PEERS;

    final Optional<OrionErrorCode> actual = OrionErrorCode.get(expected.code());

    assertTrue("Expecting an error code to be returned", actual.isPresent());
    assertEquals(expected, actual.get());
  }

  @Test
  public void absent() {
    final String missingCode = "I don't really exist";

    final Optional<OrionErrorCode> actual = OrionErrorCode.get(missingCode);

    assertFalse("Expecting no error code to be returned", actual.isPresent());
  }
}
