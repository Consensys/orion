package net.consensys.orion.api.enclave;

import java.util.Optional;

/**
 * KeyConfig is the public API to use for creating a new key. The implementation of the key store will store the key at
 * the base path. The actual usage of this will be implementation dependant, but in the current initial json storage
 * system it will use it to create two files: a basePath.key, and basePath.pub, with the .key containing the private
 * key, and the .pub with the public key.
 */
public class KeyConfig {
  private final String basePath;
  private final Optional<String> password;

  /**
   * Create the config.
   *
   * @param basePath Basepath to use for this config
   * @param password Optional password to use.
   */
  public KeyConfig(String basePath, Optional<String> password) {
    this.basePath = basePath;
    this.password = password;
  }

  public String basePath() {
    return basePath;
  }

  public Optional<String> password() {
    return password;
  }
}
