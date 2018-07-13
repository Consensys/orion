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
import static net.consensys.cava.io.Base64.decodeBytes;
import static net.consensys.cava.io.Base64.encodeBytes;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.crypto.sodium.SodiumException;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.enclave.KeyStore;
import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Serializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

public class FileKeyStore implements KeyStore {

  private final Map<Box.PublicKey, Box.SecretKey> cache;
  private final Box.PublicKey[] alwaysSendTo;
  private final Box.PublicKey[] nodeKeys;

  /**
   * Initialize the key store, loading keys specified in the configuration.
   *
   * @param config The configuration.
   * @throws IOException If an I/O error occurs loading any keys.
   */
  public FileKeyStore(Config config) throws IOException {
    cache = loadKeyPairsFromConfig(config);
    alwaysSendTo = loadPublicKeysFromConfig(config.alwaysSendTo());
    nodeKeys = loadPublicKeysFromConfig(config.publicKeys());
  }

  private Map<Box.PublicKey, Box.SecretKey> loadKeyPairsFromConfig(Config config) throws IOException {
    final Optional<Path> passwords = config.passwords();
    final List<String> passwordList;
    if (passwords.isPresent()) {
      passwordList = readPasswords(passwords.get());
    } else {
      passwordList = Collections.emptyList();
    }

    List<Path> publicKeys = config.publicKeys();
    List<Path> privateKeys = config.privateKeys();
    if (publicKeys.size() != privateKeys.size()) {
      throw new IllegalStateException("Config should have validated that key sets have the same size");
    }

    Map<Box.PublicKey, Box.SecretKey> keys = new HashMap<>();
    for (int i = 0; i < publicKeys.size(); i++) {
      final Path publicKeyFile = publicKeys.get(i);
      final Path privateKeyFile = privateKeys.get(i);
      final String password = (i < passwordList.size()) ? passwordList.get(i) : null;
      final Box.PublicKey publicKey = readPublicKey(publicKeyFile);
      keys.put(publicKey, readPrivateKey(privateKeyFile, password));
    }
    return keys;
  }

  private Box.PublicKey[] loadPublicKeysFromConfig(List<Path> paths) throws IOException {
    Box.PublicKey[] keys = new Box.PublicKey[paths.size()];
    int i = 0;
    for (Path path : paths) {
      keys[i++] = readPublicKey(path);
    }
    return keys;
  }

  private Box.SecretKey readPrivateKey(Path privateKeyFile, @Nullable String password) throws IOException {
    final StoredPrivateKey storedPrivateKey;
    try {
      storedPrivateKey = Serializer.readFile(HttpContentType.JSON, privateKeyFile, StoredPrivateKey.class);
    } catch (IOException ex) {
      throw new IOException("Failed to read private key file '" + privateKeyFile.toAbsolutePath() + "'", ex);
    }
    return storedPrivateKey.toSecretKey(password);
  }

  private Box.PublicKey readPublicKey(Path publicKeyFile) throws IOException {
    try (BufferedReader br = Files.newBufferedReader(publicKeyFile, UTF_8)) {
      final String base64Encoded = br.readLine();
      final byte[] decoded = decodeBytes(base64Encoded);
      return Box.PublicKey.fromBytes(decoded);
    } catch (final IOException ex) {
      throw new IOException("Failed to read public key file '" + publicKeyFile.toAbsolutePath() + "'", ex);
    }
  }

  private List<String> readPasswords(Path passwords) throws IOException {
    try {
      return Files.readAllLines(passwords);
    } catch (final IOException ex) {
      throw new IOException("Failed to read password list '" + passwords.toAbsolutePath() + "'", ex);
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
   * @throws IOException If an I/O error occurs.
   */
  public Box.PublicKey generateKeyPair(Path basePath) throws IOException {
    return generateKeyPair(basePath, null);
  }

  /**
   * Generate and put a new keypair, returning the public key for external use.
   *
   * @param basePath The basename and path for the generated <code>.pub</code> and <code>.key</code> files.
   * @param password The password for encrypting the key file.
   * @return Return the public key part of the key pair.
   * @throws IOException If an I/O error occurs.
   */
  public Box.PublicKey generateKeyPair(Path basePath, @Nullable String password) throws IOException {
    final Box.KeyPair keyPair = keyPair();
    final Path publicFile = basePath.resolveSibling(basePath.getFileName() + ".pub");
    final Path privateFile = basePath.resolveSibling(basePath.getFileName() + ".key");
    storePublicKey(keyPair.publicKey(), publicFile);
    final StoredPrivateKey privKey = StoredPrivateKey.fromSecretKey(keyPair.secretKey(), password);
    storePrivateKey(privKey, privateFile);
    cache.put(keyPair.publicKey(), keyPair.secretKey());
    return keyPair.publicKey();
  }

  private Box.KeyPair keyPair() {
    try {
      return Box.KeyPair.random();
    } catch (final SodiumException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_CREATE_KEY_PAIR, e);
    }
  }

  private void storePrivateKey(StoredPrivateKey privKey, Path privateFile) throws IOException {
    try {
      Serializer.writeFile(HttpContentType.JSON, privateFile, privKey);
    } catch (IOException ex) {
      throw new IOException("Failed writing private key to " + privateFile, ex);
    }
  }

  private void storePublicKey(Box.PublicKey publicKey, Path publicFile) throws IOException {
    try (Writer fw = Files.newBufferedWriter(publicFile, UTF_8)) {
      try {
        fw.write(encodeBytes(publicKey.bytesArray()));
      } catch (IOException ex) {
        throw new IOException("Failed writing public key to " + publicFile, ex);
      }
    }
  }

  @Override
  public Box.PublicKey[] alwaysSendTo() {
    return alwaysSendTo;
  }

  @Override
  public Box.PublicKey[] nodeKeys() {
    return nodeKeys;
  }
}
