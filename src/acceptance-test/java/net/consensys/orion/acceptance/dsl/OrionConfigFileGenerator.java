/*
 * Copyright 2019 ConsenSys AG.
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
package net.consensys.orion.acceptance.dsl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

public class OrionConfigFileGenerator {

  private final List<KeyDefinition> keys;
  private final String libSodiumPath;
  private final List<String> bootnodeClientUrl;
  private final Path workPath;

  public static final String CONFIG_FILENAME = "config.toml";
  public static final String networkInterface = "127.0.0.1";
  public static final String URL_PATTERN = "http://" + networkInterface + ":%d";

  public OrionConfigFileGenerator(
      final List<KeyDefinition> keys,
      final String libSodiumPath,
      final List<String> bootnodeClientUrl,
      final Path workPath) {
    this.keys = keys;
    this.libSodiumPath = libSodiumPath;
    this.bootnodeClientUrl = bootnodeClientUrl;
    this.workPath = workPath;
  }

  public Path generateConfigFile() throws IOException {
    final String pubKeys = keys.stream().map(k -> "\"" + k.getPublicKeyPath().getFileName().toString() + "\"").collect(
        Collectors.joining(","));
    final String privKeys =
        keys.stream().map(k -> "\"" + k.getPrivateKeyPath().getFileName().toString() + "\"").collect(
            Collectors.joining(","));

    final String otherNodes = bootnodeClientUrl.stream().map(k -> "\"" + k + "\"").collect(Collectors.joining(","));

    // 0 means Vertx will find a suitable port.
    String configContent = "workdir = \"" + workPath.toString() + "\"\n";
    configContent += "nodeUrl = \"" + String.format(URL_PATTERN, 0) + "\"\n";
    configContent += "clientUrl = \"" + String.format(URL_PATTERN, 0) + "\"\n";
    configContent += "nodenetworkinterface = \"" + networkInterface + "\"\n";
    configContent += "clientnetworkinterface = \"" + networkInterface + "\"\n";
    configContent += "libsodiumPath = \"" + libSodiumPath + "\"\n";
    configContent += "nodeport = 0\n";
    configContent += "clientport = 0\n";

    configContent += "publickeys = [" + pubKeys + "]\n";
    configContent += "privatekeys = [" + privKeys + "]\n";
    configContent += "othernodes  = [" + otherNodes + "]\n";

    final File configFile = new File(workPath.toFile(), CONFIG_FILENAME);
    Files.write(configFile.toPath(), configContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);

    return configFile.toPath();
  }


}
