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

package net.consensys.orion.api.enclave;

import java.nio.file.Path;
import java.util.Optional;

/**
 * KeyConfig is the public API to use for creating a new key. The implementation of the key store will store the key at
 * the base path. The actual usage of this will be implementation dependant, but in the current initial json storage
 * system it will use it to create two files: a basePath.key, and basePath.pub, with the .key containing the private
 * key, and the .pub with the public key.
 */
public class KeyConfig {
  private final Path basePath;
  private final Optional<String> password;

  /**
   * Create the config.
   *
   * @param basePath Basepath to use for this config
   * @param password Optional password to use.
   */
  public KeyConfig(Path basePath, Optional<String> password) {
    this.basePath = basePath;
    this.password = password;
  }

  public Path basePath() {
    return basePath;
  }

  public Optional<String> password() {
    return password;
  }
}
