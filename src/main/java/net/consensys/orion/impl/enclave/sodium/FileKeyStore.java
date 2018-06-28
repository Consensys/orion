package net.consensys.orion.impl.enclave.sodium;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.crypto.sodium.PasswordHash;
import net.consensys.cava.crypto.sodium.SecretBox;
import net.consensys.cava.crypto.sodium.SodiumException;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.enclave.KeyConfig;
import net.consensys.orion.api.enclave.KeyStore;
import net.consensys.orion.api.enclave.PrivateKey;
import net.consensys.orion.api.enclave.PublicKey;
import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.impl.enclave.sodium.storage.ArgonOptions;
import net.consensys.orion.impl.enclave.sodium.storage.PrivateKeyData;
import net.consensys.orion.impl.enclave.sodium.storage.SodiumArgon2Sbox;
import net.consensys.orion.impl.enclave.sodium.storage.StoredPrivateKey;
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

import org.jetbrains.annotations.NotNull;

public class FileKeyStore implements KeyStore {

  private final Config config;

  private final Map<PublicKey, PrivateKey> cache = new HashMap<>();

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
      final Optional<String> password = passwordList.isPresent() ? Optional.of(passwordList.get()[i]) : empty();
      final PublicKey publicKey = readPublicKey(publicKeyFile);
      final PrivateKey privateKey = readPrivateKey(privateKeyFile, password);

      cache.put(publicKey, privateKey);
    }
  }

  private PrivateKey readPrivateKey(Path privateKeyFile, Optional<String> password) {
    final StoredPrivateKey storedPrivateKey =
        Serializer.readFile(HttpContentType.JSON, privateKeyFile, StoredPrivateKey.class);

    Box.SecretKey key;
    switch (storedPrivateKey.type()) {
      case StoredPrivateKey.UNLOCKED:
        key = Box.SecretKey.fromBytes(Base64.decode(storedPrivateKey.data().bytes().get()));
        break;
      case StoredPrivateKey.ARGON2_SBOX:
        if (!password.isPresent()) {
          throw new EnclaveException(
              OrionErrorCode.ENCLAVE_MISSING_PRIVATE_KEY_PASSWORD,
              "missing password to read private key");
        }
        byte[] cipherText = Base64.decode(storedPrivateKey.data().sbox().get());
        PasswordHash.Salt salt = PasswordHash.Salt.fromBytes(Base64.decode(storedPrivateKey.data().asalt().get()));
        SecretBox.Nonce nonce = SecretBox.Nonce.fromBytes(Base64.decode(storedPrivateKey.data().snonce().get()));
        final ArgonOptions argonOptions = storedPrivateKey.data().aopts().get();
        key = SodiumArgon2Sbox.decrypt(cipherText, password.get(), salt, nonce, argonOptions);
        break;
      default:
        throw new EnclaveException(
            OrionErrorCode.ENCLAVE_UNSUPPORTED_PRIVATE_KEY_TYPE,
            "Unable to support private key storage of type: " + storedPrivateKey.type());
    }
    return new PrivateKey(key.bytesArray());
  }

  private PublicKey readPublicKey(Path publicKeyFile) {
    try (BufferedReader br = Files.newBufferedReader(publicKeyFile, UTF_8)) {
      final String base64Encoded = br.readLine();
      final byte[] decoded = Base64.decode(base64Encoded);
      return new PublicKey(decoded);
    } catch (final IOException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_READ_PUBLIC_KEY, e);
    }
  }

  @Override
  public Optional<PrivateKey> privateKey(PublicKey publicKey) {
    return Optional.ofNullable(cache.get(publicKey));
  }

  @Override
  public PublicKey generateKeyPair(KeyConfig config) {
    final Path basePath = config.basePath();
    final Optional<String> password = config.password();
    return generateStoreAndCache(basePath, password);
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

  private PublicKey generateStoreAndCache(Path baseName, Optional<String> password) {
    final Box.KeyPair keyPair = keyPair();
    final Path publicFile = baseName.resolveSibling(baseName.getFileName() + ".pub");
    final Path privateFile = baseName.resolveSibling(baseName.getFileName() + ".key");
    storePublicKey(keyPair.publicKey(), publicFile);
    final StoredPrivateKey privKey = createStoredPrivateKey(keyPair, password);
    storePrivateKey(privKey, privateFile);
    final PublicKey publicKey = new PublicKey(keyPair.publicKey().bytesArray());
    final PrivateKey privateKey = new PrivateKey(keyPair.secretKey().bytesArray());
    cache.put(publicKey, privateKey);
    return publicKey;
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

  @NotNull
  private StoredPrivateKey createStoredPrivateKey(Box.KeyPair keyPair, Optional<String> password) {
    final PrivateKeyData data;

    if (password.isPresent()) {
      final ArgonOptions argonOptions = defaultArgonOptions();
      final SecretBox.Nonce nonce = SecretBox.Nonce.random();
      final PasswordHash.Salt salt = PasswordHash.Salt.random();
      final byte[] cipherText =
          SodiumArgon2Sbox.encrypt(keyPair.secretKey(), password.get(), salt, nonce, argonOptions);
      data = new PrivateKeyData(
          Optional.empty(),
          of(Base64.encode(salt.bytesArray())),
          of(argonOptions),
          of(Base64.encode(nonce.bytesArray())),
          of(Base64.encode(cipherText)));
      return new StoredPrivateKey(data, StoredPrivateKey.ARGON2_SBOX);
    } else {
      data = new PrivateKeyData(Base64.encode(keyPair.secretKey().bytesArray()));
      return new StoredPrivateKey(data, StoredPrivateKey.UNLOCKED);
    }
  }

  @NotNull
  private ArgonOptions defaultArgonOptions() {
    return new ArgonOptions(
        "i",
        ArgonOptions.VERSION,
        empty(),
        empty(),
        empty(),
        of(ArgonOptions.OPS_LIMIT_MODERATE),
        of(ArgonOptions.MEM_LIMIT_MODERATE));
  }

  @Override
  public PublicKey[] alwaysSendTo() {
    return config.alwaysSendTo().stream().map(this::readPublicKey).toArray(PublicKey[]::new);
  }

  @Override
  public PublicKey[] nodeKeys() {
    return config.publicKeys().stream().map(this::readPublicKey).toArray(PublicKey[]::new);
  }
}
