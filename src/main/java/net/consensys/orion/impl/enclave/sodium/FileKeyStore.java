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

package net.consensys.orion.impl.enclave.sodium;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.empty;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.crypto.sodium.SodiumException;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.enclave.KeyStore;
import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Base64;
import net.consensys.orion.impl.utils.Serializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

public class FileKeyStore implements KeyStore {

  private final Config config;

  private final Map<Box.PublicKey, Box.SecretKey> cache = new HashMap<>();

  public FileKeyStore(Config config) {
    this.config = config;
    // load keys
    loadKeysFromConfig(config);
  }

  private void loadKeysFromConfig(Config config) {
    final Optional<String[]> passwordList = lookupPasswords();

    List<Path> publicKeys = config.publicKeys();
    List<Path> privateKeys = config.privateKeys();
    for (int i = 0; i < publicKeys.size(); i++) {
      final Path publicKeyFile = publicKeys.get(i);
      final Path privateKeyFile = privateKeys.get(i);
      final Box.PublicKey publicKey = readPublicKey(publicKeyFile);
      cache.put(publicKey, readPrivateKey(privateKeyFile, passwordList.isPresent() ? passwordList.get()[i] : null));
    }
  }

  private Box.SecretKey readPrivateKey(Path privateKeyFile, @Nullable String password) {
    final StoredPrivateKey storedPrivateKey =
        Serializer.readFile(HttpContentType.JSON, privateKeyFile, StoredPrivateKey.class);
    return storedPrivateKey.toSecretKey(password);
  }

  private Box.PublicKey readPublicKey(Path publicKeyFile) {
    try (BufferedReader br = Files.newBufferedReader(publicKeyFile, UTF_8)) {
      final String base64Encoded = br.readLine();
      final byte[] decoded = Base64.decode(base64Encoded);
      return Box.PublicKey.fromBytes(decoded);
    } catch (final IOException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_READ_PUBLIC_KEY, e);
    }
  }

  @Override
  @Nullable
  public Box.SecretKey privateKey(Box.PublicKey publicKey) {
    return cache.get(publicKey);
  }

  /**
   * Generate and put a new keypair, returning the public key for external use.
   *
   * @param basePath The basename and path for the generated <code>.pub</code> and <code>.key</code> files.
   * @return Return the public key part of the key pair.
   */
  public Box.PublicKey generateKeyPair(Path basePath) {
    return generateKeyPair(basePath, null);
  }

  /**
   * Generate and put a new keypair, returning the public key for external use.
   *
   * @param basePath The basename and path for the generated <code>.pub</code> and <code>.key</code> files.
   * @param password The password for encrypting the key file.
   * @return Return the public key part of the key pair.
   */
  public Box.PublicKey generateKeyPair(Path basePath, @Nullable String password) {
    final Box.KeyPair keyPair = keyPair();
    final Path publicFile = basePath.resolveSibling(basePath.getFileName() + ".pub");
    final Path privateFile = basePath.resolveSibling(basePath.getFileName() + ".key");
    storePublicKey(keyPair.publicKey(), publicFile);
    final StoredPrivateKey privKey = StoredPrivateKey.fromSecretKey(keyPair.secretKey(), password);
    storePrivateKey(privKey, privateFile);
    cache.put(keyPair.publicKey(), keyPair.secretKey());
    return keyPair.publicKey();
  }

  private Optional<String[]> lookupPasswords() {
    final Optional<Path> passwords = config.passwords();

    if (passwords.isPresent()) {
      try {
        final List<String> strings = Files.readAllLines(passwords.get());
        return Optional.of(strings.toArray(new String[0]));
      } catch (final IOException e) {
        throw new EnclaveException(OrionErrorCode.ENCLAVE_READ_PASSWORDS, e);
      }
    }

    return empty();
  }

  private Box.KeyPair keyPair() {
    try {
      return Box.KeyPair.random();
    } catch (final SodiumException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_CREATE_KEY_PAIR, e);
    }
  }

  private void storePrivateKey(StoredPrivateKey privKey, Path privateFile) {
    Serializer.writeFile(HttpContentType.JSON, privateFile, privKey);
  }

  private void storePublicKey(Box.PublicKey publicKey, Path publicFile) {
    try (Writer fw = Files.newBufferedWriter(publicFile, UTF_8)) {
      fw.write(Base64.encode(publicKey.bytesArray()));
    } catch (final IOException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_WRITE_PUBLIC_KEY, e);
    }
  }

  @Override
  public Box.PublicKey[] alwaysSendTo() {
    return config.alwaysSendTo().stream().map(this::readPublicKey).toArray(Box.PublicKey[]::new);
  }

  @Override
  public Box.PublicKey[] nodeKeys() {
    return config.publicKeys().stream().map(this::readPublicKey).toArray(Box.PublicKey[]::new);
  }
}
