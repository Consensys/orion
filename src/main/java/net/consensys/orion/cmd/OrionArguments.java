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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Optional;
import java.util.stream.Collectors;

class OrionArguments {
  private boolean argumentExit = false;

  private Optional<String> configFileName = Optional.empty();
  private Optional<String[]> keysToGenerate = Optional.empty();

  OrionArguments(final PrintStream out, final PrintStream err, final String[] args) {

    boolean showUsage = false;
    // Process Arguments
    // Usage Orion [--generatekeys|-g names] [config]
    // names - comma seperated list of key file prefixes (can include directory information) to
    // generate key(s) for
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--generatekeys":
        case "-g":
          if (++i >= args.length) {
            err.println("Error: Missing key names to generate.");
            argumentExit = true;
            showUsage = true;
            break;
          }
          final String keys = args[i];
          keysToGenerate = Optional.of(keys.split(","));
          break;
        case "--help":
        case "-h":
          argumentExit = true;
          showUsage = true;
          break;
        case "--version":
        case "-v":
          displayVersion(out, err);
          argumentExit = true;
          break;
        default:
          if (args[i].startsWith("-")) {
            err.printf("Invalid option: %s\n", args[i]);
            argumentExit = true;
            showUsage = true;
          } else {
            configFileName = Optional.of(args[i]);
          }
      }
    }

    if (showUsage) {
      displayHelp(out);
    }
  }

  private void displayHelp(final PrintStream out) {
    out.println("Usage: " + Orion.NAME + " [options] [config file]");
    out.println("where options include:");
    out.println("\t-g");
    out.println("\t--generatekeys <names>");
    out.println("\t\tgenerate key pairs for each of the names supplied.");
    out.println("\t\twhere <names> are a comma-seperated list");
    out.println("\t-h");
    out.println("\t--help\tprint this help message");
    out.println("\t-v");
    out.println("\t--version\tprint version information");
  }

  private void displayVersion(final PrintStream out, final PrintStream err) {
    try (final InputStream versionAsStream = OrionArguments.class.getResourceAsStream("/version.txt")) {
      if (versionAsStream == null) {
        out.println("(development)");
      } else {
        final BufferedReader buffer = new BufferedReader(new InputStreamReader(versionAsStream, UTF_8));
        final String contents = buffer.lines().collect(Collectors.joining("\n"));
        out.println(contents);
      }
    } catch (final IOException e) {
      err.println("Read of Version file failed " + e.getMessage());
    }
  }

  boolean argumentExit() {
    return argumentExit;
  }

  Optional<String> configFileName() {
    return configFileName;
  }

  Optional<String[]> keysToGenerate() {
    return keysToGenerate;
  }
}
