package net.consensys.athena.impl.cmd;

import java.util.Optional;

public class AthenaArguments {
  private boolean argumentExit = false;

  private Optional<String> configFileName = Optional.empty();
  private Optional<String[]> keysToGenerate = Optional.empty();

  public AthenaArguments(String[] args) {

    // Process Arguments
    // Usage Athena [--generatekeys|-g names] [config]
    // names - comma seperated list of key file prefixes (can include directory information) to
    // generate key(s) for
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--generatekeys":
        case "-g":
          if (++i >= args.length) {
            System.out.println("Error: Missing key names to generate.");
            argumentExit = true;
            break;
          }
          String keys = args[i];
          keysToGenerate = Optional.of(keys.split(","));
          break;
        default:
          configFileName = Optional.of(args[i]);
      }
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
