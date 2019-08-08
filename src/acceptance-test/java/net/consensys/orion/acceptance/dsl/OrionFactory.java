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

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.orion.config.Config;
import net.consensys.orion.enclave.sodium.FileKeyStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OrionFactory {

  private static final Logger LOG = LogManager.getLogger();

  private static final String libSodiumPath = "/usr/local/lib/libsodium.dylib";


  public OrionNode create(final String nodeName, int keyCount) throws IOException {
    return create(nodeName, keyCount, Collections.emptyList());
  }

  public OrionNode create(final String nodeName, final int keyCount, final List<OrionNode> bootnodes)
      throws IOException {
    final Path nodePath;
    try {
      nodePath = Files.createTempDirectory("orion");
    } catch (final IOException e) {
      LOG.error("Failed to create tmpdir for orion instance.");
      throw e;
    }

    final Config config = Config.defaultConfig();
    try {
      final FileKeyStore fileKeyStore = new FileKeyStore(config);

      final List<KeyDefinition> nodeKeys = Lists.newArrayList();
      for (int i = 0; i < keyCount; i++) {
        final String keyFileName = String.format("key_%d", i);
        final Path rootPath = nodePath.resolve(keyFileName);
        Box.PublicKey pubKey = fileKeyStore.generateKeyPair(rootPath, null);

        final KeyDefinition keys = new KeyDefinition(
            new Box.KeyPair(pubKey, fileKeyStore.privateKey(pubKey)),
            rootPath.resolveSibling(rootPath.getFileName() + ".pub"),
            rootPath.resolveSibling(rootPath.getFileName() + ".key"));
        nodeKeys.add(keys);
      }

      final List<String> bootnodeStrings = bootnodes.stream().map(OrionNode::nodeAddress).collect(Collectors.toList());

      return new OrionNode(nodeName, nodeKeys, nodePath, libSodiumPath, bootnodeStrings);

    } catch (final IOException e) {
      LOG.error("Failed to create filekeyStore");
      throw e;
    }
  }

}
