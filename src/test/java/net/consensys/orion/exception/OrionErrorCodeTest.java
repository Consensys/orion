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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
