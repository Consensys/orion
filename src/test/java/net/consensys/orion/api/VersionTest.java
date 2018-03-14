package net.consensys.orion.api;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class VersionTest {
  /**
   * A simple placeholder test that shows tests are running. Show that the API Version is the
   * expected version. It is expected that a good cleanup of code will result in this being deleted.
   */
  @Test
  public void versionIs0_0_0_1() {
    assertArrayEquals(Version.VERSION, new int[] {0, 0, 0, 1});
  }
}
