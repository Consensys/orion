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
import net.consensys.cava.crypto.sodium.Box.KeyPair;

import java.nio.file.Path;

public class KeyDefinition {

  final private Box.KeyPair keys;
  final private Path publicKeyPath;
  final private Path privateKeyPath;

  public KeyDefinition(KeyPair keys, Path publicKeyPath, Path privateKeyPath) {
    this.keys = keys;
    this.publicKeyPath = publicKeyPath;
    this.privateKeyPath = privateKeyPath;
  }

  public KeyPair getKeys() {
    return keys;
  }

  public Path getPublicKeyPath() {
    return publicKeyPath;
  }

  public Path getPrivateKeyPath() {
    return privateKeyPath;
  }
}
