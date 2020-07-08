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
package net.consensys.orion.cmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.orion.utils.OrionInfo;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

class OrionArgumentsTest {

  private final String usageOut = String.format(
      "Usage: "
          + Orion.NAME
          + " [options] [config file]%n"
          + "where options include:%n"
          + "\t-g%n"
          + "\t--generatekeys <names>%n\t\tgenerate key pairs for each of the names supplied%n\t\twhere <names> are a comma-separated list%n"
          + "\t--clear-known-nodes\tclear known nodes information.%n"
          + "\t-h%n"
          + "\t--help\tprint this help message%n"
          + "\t-v%n"
          + "\t--version\tprint version information%n");

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
  private final PrintStream outStream = new PrintStream(outContent);
  private final PrintStream errStream = new PrintStream(errContent);

  @Test
  void generateKeysArgumentWithNoKeyNamesProvided() {
    final String errorMsg = String.format("Error: Missing key names to generate.%n");
    final String[] args = {"-g"};

    final OrionArguments arguments = new OrionArguments(outStream, errStream, args);

    assertEquals(errorMsg, errContent.toString());
    assertEquals(usageOut, outContent.toString());
    assertTrue(arguments.argumentExit());
  }

  @Test
  void helpOutput() {
    final String[] args = {"--help"};

    final OrionArguments arguments = new OrionArguments(outStream, errStream, args);

    assertEquals(usageOut, outContent.toString());
    assertTrue(arguments.argumentExit());
  }

  @Test
  void versionArgument() {
    final String[] args = {"-v"};
    final OrionArguments arguments = new OrionArguments(outStream, errStream, args);

    final String expectedVersion = OrionInfo.version();

    assertEquals(expectedVersion + System.lineSeparator(), outContent.toString());
    assertTrue(arguments.argumentExit());
  }

  @Test
  void invalidOption() {
    final String errorMsg = "Invalid option: -x\n";
    final String[] args = {"-x"};

    final OrionArguments arguments = new OrionArguments(outStream, errStream, args);

    assertEquals(errorMsg, errContent.toString());
    assertEquals(usageOut, outContent.toString());
    assertTrue(arguments.argumentExit());
  }

  @Test
  void clearKnownNodeDefaultOption() {
    final String[] args = {};

    final OrionArguments arguments = new OrionArguments(outStream, errStream, args);

    assertEquals("", errContent.toString());
    assertEquals("", outContent.toString());
    assertFalse(arguments.clearKnownNodes());
  }

  @Test
  void clearKnownNodes() {
    final String[] args = {"--clear-known-nodes"};

    final OrionArguments arguments = new OrionArguments(outStream, errStream, args);

    assertEquals("", errContent.toString());
    assertEquals("", outContent.toString());
    assertTrue(arguments.clearKnownNodes());
  }

  @Test
  void validAndInvalidOptions() {
    final String errorMsg = "Invalid option: -x\n";
    final String[] args = {"-x", "-g", "keys"};

    final OrionArguments arguments = new OrionArguments(outStream, errStream, args);

    assertEquals(errorMsg, errContent.toString());
    assertEquals(usageOut, outContent.toString());
    assertTrue(arguments.argumentExit());
  }

  @Test
  void configFileParam() {
    final String[] args = {"config.conf"};

    final OrionArguments arguments = new OrionArguments(outStream, errStream, args);
    assertTrue(arguments.configFileName().isPresent());
    assertEquals("config.conf", arguments.configFileName().get());
  }
}
