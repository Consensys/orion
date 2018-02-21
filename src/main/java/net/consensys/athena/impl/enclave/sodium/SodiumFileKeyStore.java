package net.consensys.athena.impl.enclave.sodium;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.api.enclave.EnclaveException;
import net.consensys.athena.api.enclave.KeyConfig;
import net.consensys.athena.api.enclave.KeyStore;
import net.consensys.athena.impl.enclave.sodium.storage.ArgonOptions;
import net.consensys.athena.impl.enclave.sodium.storage.PrivateKeyData;
import net.consensys.athena.impl.enclave.sodium.storage.SodiumArgon2Sbox;
import net.consensys.athena.impl.enclave.sodium.storage.StoredPrivateKey;
import net.consensys.athena.impl.http.server.HttpContentType;
import net.consensys.athena.impl.utils.Base64;
import net.consensys.athena.impl.utils.Serializer;

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
    Optional<String[]> passwordList = lookupPasswords();

    for (int i = 0; i < config.publicKeys().length; i++) {
      File publicKeyFile = config.publicKeys()[i];
      File privateKeyFile = config.privateKeys()[i];
      Optional<String> password = empty();
      if (passwordList.isPresent()) {
        password = Optional.of(passwordList.get()[i]);
      }
      PublicKey publicKey = readPublicKey(publicKeyFile);
      PrivateKey privateKey = readPrivateKey(privateKeyFile, password);

      cache.put(publicKey, privateKey);
    }
  }

  private PrivateKey readPrivateKey(File privateKeyFile, Optional<String> password) {
    StoredPrivateKey storedPrivateKey =
        serializer.readFile(HttpContentType.JSON, privateKeyFile, StoredPrivateKey.class);

    byte[] decoded;
    switch (storedPrivateKey.type()) {
      case StoredPrivateKey.UNLOCKED:
        decoded = Base64.decode(storedPrivateKey.data().bytes().get());
        break;
      case StoredPrivateKey.ARGON2_SBOX:
        if (!password.isPresent()) {
          throw new EnclaveException("missing password to read private key");
        }
        decoded = new SodiumArgon2Sbox(config).decrypt(storedPrivateKey, password.get());
        break;
      default:
        throw new EnclaveException(
            "Unable to support private key storage of type: " + storedPrivateKey.type());
    }
    return new SodiumPrivateKey(decoded);
  }

  private PublicKey readPublicKey(File publicKeyFile) {
    try (BufferedReader br = new BufferedReader(new FileReader(publicKeyFile))) {
      String base64Encoded = br.readLine();
      byte[] decoded = Base64.decode(base64Encoded);
      return new SodiumPublicKey(decoded);
    } catch (IOException e) {
      throw new EnclaveException(e);
    }
  }

  @Override
  public PrivateKey privateKey(PublicKey publicKey) {
    return cache.get(publicKey);
  }

  @Override
  public PublicKey generateKeyPair(KeyConfig config) {
    String basePath = config.basePath();
    Optional<String> password = config.password();
    return generateStoreAndCache(basePath, password);
  }

  private Optional<String[]> lookupPasswords() {
    Optional<File> passwords = config.passwords();
    Optional<String[]> passwordList = empty();
    if (passwords.isPresent()) {
      try {
        List<String> strings = Files.readAllLines(passwords.get().toPath());
        passwordList = Optional.of(strings.toArray(new String[strings.size()]));
      } catch (IOException e) {
        throw new EnclaveException(e);
      }
    }
    return passwordList;
  }

  private PublicKey generateStoreAndCache(String key, Optional<String> password) {
    try {
      SodiumKeyPair keyPair = SodiumLibrary.cryptoBoxKeyPair();
      File publicFile = new File(key + ".pub");
      File privateFile = new File(key + ".key");
      storePublicKey(keyPair.getPublicKey(), publicFile);
      StoredPrivateKey privKey = createStoredPrivateKey(keyPair, password);
      storePrivateKey(privKey, privateFile);
      SodiumPublicKey publicKey = new SodiumPublicKey(keyPair.getPublicKey());
      SodiumPrivateKey privateKey = new SodiumPrivateKey(keyPair.getPrivateKey());
      cache.put(publicKey, privateKey);
      return publicKey;
    } catch (SodiumLibraryException e) {
      throw new EnclaveException(e);
    }
  }

  private void storePrivateKey(StoredPrivateKey privKey, File privateFile) {
    serializer.writeFile(HttpContentType.JSON, privateFile, privKey);
  }

  private void storePublicKey(byte[] publicKey, File publicFile) {
    try (FileWriter fw = new FileWriter(publicFile)) {
      fw.write(Base64.encode(publicKey));
    } catch (IOException e) {
      throw new EnclaveException(e);
    }
  }

  @NotNull
  private StoredPrivateKey createStoredPrivateKey(
      SodiumKeyPair keyPair, Optional<String> password) {
    PrivateKeyData data;

    if (password.isPresent()) {
      ArgonOptions argonOptions = defaultArgonOptions();
      SodiumArgon2Sbox sodiumArgon2Sbox = new SodiumArgon2Sbox(config);
      byte[] snonce = sodiumArgon2Sbox.generateSnonce();
      byte[] asalt = sodiumArgon2Sbox.generateAsalt();
      byte[] sbox =
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
    File[] alwaysSendTo = config.alwaysSendTo();
    return Arrays.stream(alwaysSendTo).map(this::readPublicKey).toArray(PublicKey[]::new);
  }

  @Override
  public PublicKey[] nodeKeys() {
    File[] publicKeys = config.publicKeys();
    return Arrays.stream(publicKeys).map(this::readPublicKey).toArray(PublicKey[]::new);
  }
}
