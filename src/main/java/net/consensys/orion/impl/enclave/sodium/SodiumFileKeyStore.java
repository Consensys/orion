package net.consensys.orion.impl.enclave.sodium;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.enclave.KeyConfig;
import net.consensys.orion.api.enclave.KeyStore;
import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.impl.enclave.sodium.storage.ArgonOptions;
import net.consensys.orion.impl.enclave.sodium.storage.PrivateKeyData;
import net.consensys.orion.impl.enclave.sodium.storage.SodiumArgon2Sbox;
import net.consensys.orion.impl.enclave.sodium.storage.StoredPrivateKey;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Base64;
import net.consensys.orion.impl.utils.Serializer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.muquit.libsodiumjna.SodiumKeyPair;
import com.muquit.libsodiumjna.SodiumLibrary;
import com.muquit.libsodiumjna.exceptions.SodiumLibraryException;
import org.jetbrains.annotations.NotNull;

public class SodiumFileKeyStore implements KeyStore {

  private final Config config;
  private final Serializer serializer;

  private final Map<PublicKey, PrivateKey> cache = new HashMap<>();

  public SodiumFileKeyStore(Config config, Serializer serializer) {
    this.config = config;
    this.serializer = serializer;
    SodiumLibrary.setLibraryPath(config.libSodiumPath());
    // load keys
    loadKeysFromConfig(config);
  }

  private void loadKeysFromConfig(Config config) {
    final Optional<String[]> passwordList = lookupPasswords();

    for (int i = 0; i < config.publicKeys().length; i++) {
      final File publicKeyFile = config.publicKeys()[i];
      final File privateKeyFile = config.privateKeys()[i];
      final Optional<String> password =
          passwordList.isPresent() ? Optional.of(passwordList.get()[i]) : empty();
      final PublicKey publicKey = readPublicKey(publicKeyFile);
      final PrivateKey privateKey = readPrivateKey(privateKeyFile, password);

      cache.put(publicKey, privateKey);
    }
  }

  private PrivateKey readPrivateKey(File privateKeyFile, Optional<String> password) {
    final StoredPrivateKey storedPrivateKey =
        serializer.readFile(HttpContentType.JSON, privateKeyFile, StoredPrivateKey.class);

    final byte[] decoded;
    switch (storedPrivateKey.type()) {
      case StoredPrivateKey.UNLOCKED:
        decoded = Base64.decode(storedPrivateKey.data().bytes().get());
        break;
      case StoredPrivateKey.ARGON2_SBOX:
        if (!password.isPresent()) {
          throw new EnclaveException(
              OrionErrorCode.ENCLAVE_MISSING_PRIVATE_KEY_PASSWORD,
              "missing password to read private key");
        }
        decoded = new SodiumArgon2Sbox(config).decrypt(storedPrivateKey, password.get());
        break;
      default:
        throw new EnclaveException(
            OrionErrorCode.ENCLAVE_UNSUPPORTED_PRIVATE_KEY_TYPE,
            "Unable to support private key storage of type: " + storedPrivateKey.type());
    }

    return new SodiumPrivateKey(decoded);
  }

  private PublicKey readPublicKey(File publicKeyFile) {
    try (BufferedReader br = new BufferedReader(new FileReader(publicKeyFile))) {
      final String base64Encoded = br.readLine();
      final byte[] decoded = Base64.decode(base64Encoded);
      return new SodiumPublicKey(decoded);
    } catch (final IOException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_READ_PUBLIC_KEY, e);
    }
  }

  @Override
  public PrivateKey privateKey(PublicKey publicKey) {
    return cache.get(publicKey);
  }

  @Override
  public PublicKey generateKeyPair(KeyConfig config) {
    final String basePath = config.basePath();
    final Optional<String> password = config.password();
    return generateStoreAndCache(basePath, password);
  }

  private Optional<String[]> lookupPasswords() {
    final Optional<File> passwords = config.passwords();

    if (passwords.isPresent()) {
      try {
        final List<String> strings = Files.readAllLines(passwords.get().toPath());
        return Optional.of(strings.toArray(new String[strings.size()]));
      } catch (final IOException e) {
        throw new EnclaveException(OrionErrorCode.ENCLAVE_READ_PASSWORDS, e);
      }
    }

    return empty();
  }

  private PublicKey generateStoreAndCache(String key, Optional<String> password) {
    final SodiumKeyPair keyPair = keyPair();
    final File publicFile = new File(key + ".pub");
    final File privateFile = new File(key + ".key");
    storePublicKey(keyPair.getPublicKey(), publicFile);
    final StoredPrivateKey privKey = createStoredPrivateKey(keyPair, password);
    storePrivateKey(privKey, privateFile);
    final SodiumPublicKey publicKey = new SodiumPublicKey(keyPair.getPublicKey());
    final SodiumPrivateKey privateKey = new SodiumPrivateKey(keyPair.getPrivateKey());
    cache.put(publicKey, privateKey);
    return publicKey;
  }

  private SodiumKeyPair keyPair() {
    try {
      return SodiumLibrary.cryptoBoxKeyPair();
    } catch (final SodiumLibraryException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_CREATE_KEY_PAIR, e);
    }
  }

  private void storePrivateKey(StoredPrivateKey privKey, File privateFile) {
    serializer.writeFile(HttpContentType.JSON, privateFile, privKey);
  }

  private void storePublicKey(byte[] publicKey, File publicFile) {
    try (FileWriter fw = new FileWriter(publicFile)) {
      fw.write(Base64.encode(publicKey));
    } catch (final IOException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_WRITE_PUBLIC_KEY, e);
    }
  }

  @NotNull
  private StoredPrivateKey createStoredPrivateKey(
      SodiumKeyPair keyPair, Optional<String> password) {
    final PrivateKeyData data;

    if (password.isPresent()) {
      final ArgonOptions argonOptions = defaultArgonOptions();
      final SodiumArgon2Sbox sodiumArgon2Sbox = new SodiumArgon2Sbox(config);
      final byte[] snonce = sodiumArgon2Sbox.generateSnonce();
      final byte[] asalt = sodiumArgon2Sbox.generateAsalt();
      final byte[] sbox =
          sodiumArgon2Sbox.encrypt(
              keyPair.getPrivateKey(), password.get(), asalt, snonce, argonOptions);
      data =
          new PrivateKeyData(
              empty(),
              of(Base64.encode(asalt)),
              of(argonOptions),
              of(Base64.encode(snonce)),
              of(Base64.encode(sbox)));
      return new StoredPrivateKey(data, StoredPrivateKey.ARGON2_SBOX);
    } else {
      data = new PrivateKeyData(Base64.encode(keyPair.getPrivateKey()));
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

  public PublicKey[] alwaysSendTo() {
    final File[] alwaysSendTo = config.alwaysSendTo();
    return Arrays.stream(alwaysSendTo).map(this::readPublicKey).toArray(PublicKey[]::new);
  }

  @Override
  public PublicKey[] nodeKeys() {
    final File[] publicKeys = config.publicKeys();
    return Arrays.stream(publicKeys).map(this::readPublicKey).toArray(PublicKey[]::new);
  }
}
