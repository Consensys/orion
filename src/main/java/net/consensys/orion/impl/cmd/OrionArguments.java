package net.consensys.orion.impl.cmd;

import static java.nio.charset.StandardCharsets.UTF_8;

import net.consensys.orion.api.cmd.Orion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrionArguments {
  private boolean argumentExit = false;

  private Optional<String> configFileName = Optional.empty();
  private Optional<String[]> keysToGenerate = Optional.empty();

  public OrionArguments(PrintStream out, PrintStream err, String[] args) {

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
          String keys = args[i];
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

  private void displayHelp(PrintStream out) {
    out.println("Usage: " + Orion.name + " [options] [config file]");
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

  private void displayVersion(PrintStream out, PrintStream err) {
    try (InputStream versionAsStream = OrionArguments.class.getResourceAsStream("/version.txt")) {
      if (versionAsStream == null) {
        out.println("(development)");
      } else {
        BufferedReader buffer = new BufferedReader(new InputStreamReader(versionAsStream, UTF_8));
        String contents = buffer.lines().collect(Collectors.joining("\n"));
        out.println(contents);
      }
    } catch (IOException e) {
      err.println("Read of Version file failed " + e.getMessage());
    }
  }

  public boolean argumentExit() {
    return argumentExit;
  }

  public Optional<String> configFileName() {
    return configFileName;
  }

  public Optional<String[]> keysToGenerate() {
    return keysToGenerate;
  }
}
