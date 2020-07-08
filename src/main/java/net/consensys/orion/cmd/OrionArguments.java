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


import net.consensys.orion.utils.OrionInfo;

import java.io.PrintStream;
import java.util.Optional;

class OrionArguments {

  private boolean argumentExit = false;
  private boolean clearKnownNodes = false;

  private Optional<String> configFileName = Optional.empty();
  private Optional<String[]> keysToGenerate = Optional.empty();

  OrionArguments(final PrintStream out, final PrintStream err, final String[] args) {

    boolean showUsage = false;
    // Process Arguments
    // Usage Orion [--generatekeys|-g names] [config]
    // names - comma separated list of key file prefixes (can include directory information) to
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
        case "--clear-known-nodes":
          clearKnownNodes = true;
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
    out.println("\t\tgenerate key pairs for each of the names supplied");
    out.println("\t\twhere <names> are a comma-separated list");
    out.println("\t--clear-known-nodes\tclear known nodes information.");
    out.println("\t-h");
    out.println("\t--help\tprint this help message");
    out.println("\t-v");
    out.println("\t--version\tprint version information");
  }

  private void displayVersion(final PrintStream out, final PrintStream err) {
    try {
      out.println(OrionInfo.version());
    } catch (final Exception e) {
      err.println("Error reading Orion version " + e.getMessage());
    }
  }

  boolean argumentExit() {
    return argumentExit;
  }

  boolean clearKnownNodes() {
    return clearKnownNodes;
  }

  Optional<String> configFileName() {
    return configFileName;
  }

  Optional<String[]> keysToGenerate() {
    return keysToGenerate;
  }
}
