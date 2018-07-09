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

package net.consensys.orion.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class VersionTest {
  /**
   * A simple placeholder test that shows tests are running. Show that the API Version is the expected version. It is
   * expected that a good cleanup of code will result in this being deleted.
   */
  @Test
  void versionIs0_0_0_1() {
    assertArrayEquals(Version.VERSION, new int[] {0, 0, 0, 1});
  }
}
