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

import java.nio.charset.StandardCharsets;
import net.consensys.cava.crypto.sodium.Box;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Responsible for providing access to a running Orion instance via its HTTP interface such that
 * payloads can be submitted and extracted.
 *
 * It also encapsulates all aspects of the Orion Instance - such as its datapath and maintained
 * keys.
 */
public class OrionInstance {

  private static final String configFileName = "config.toml";

  private final List<KeyDefinition> keys;
  private final Path workPath;
  private final String libSodiumPath;

  public OrionInstance(final List<KeyDefinition> keys, final Path workPath,
      final String libSodiumPath) {
    this.keys = keys;
    this.workPath = workPath;
    this.libSodiumPath = libSodiumPath;
  }

  public void start() throws IOException {
    generateConfigFile();
    OrionProcessRunner runner = new OrionProcessRunner(configFileName, workPath);
    runner.start("orionNode1");

  }

  public void sendData(final byte[] data, final String sender, final List<String> recipients) {

  }

  public byte[] extractDataItem(final String dataKey, final String identity) {
    return null;
  }

  public List<Box.PublicKey> publicKeys() {
    return keys.stream().map(k -> k.getKeys().publicKey()).collect(Collectors.toList());
  }

  public List<Box.SecretKey> privateKeys() {
    return keys.stream().map(k -> k.getKeys().secretKey()).collect(Collectors.toList());
  }

  private void generateConfigFile() throws IOException {
    final String pubKeys =
        keys.stream().map(k -> "\"" + k.getPublicKeyPath().getFileName().toString() + "\"")
            .collect(Collectors.joining(","));
    final String privKeys =
        keys.stream().map(k -> "\"" + k.getPrivateKeyPath().getFileName().toString() + "\"")
            .collect(Collectors.joining(","));

    String configContent = "workdir = \"" + workPath.toString() + "\"\n";
    configContent += "nodeUrl = \"http://127.0.0.1:0\"\n"; // 0 means Vertx will find a suitable port.
    configContent += "clientPort = \"http://127.0.0.1:0\"\n";
    configContent += "nodenetworkinterface = \"127.0.0.1\"\n";
    configContent += "clientnetworkinterface = \"127.0.0.1\"\n";
    configContent += "libsodiumPath = \"" + libSodiumPath + "\"\n";

    configContent += "publickeys = [" + pubKeys + "]\n";
    configContent += "privatekeys = [" + privKeys + "]\n";

    final File configFile = new File(workPath.toFile(), configFileName);
    Files.write(configFile.toPath(), configContent.getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE);

  }


}
