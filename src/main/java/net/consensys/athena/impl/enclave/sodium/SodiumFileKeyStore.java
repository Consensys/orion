package net.consensys.athena.impl.enclave.sodium;

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
      Optional<String> password = Optional.empty();
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
    switch (storedPrivateKey.getType()) {
      case StoredPrivateKey.UNLOCKED:
        decoded = Base64.decode(storedPrivateKey.getData().getBytes());
        break;
      case StoredPrivateKey.ARGON2_SBOX:
        decoded = new SodiumArgon2Sbox(config).decrypt(storedPrivateKey, password.get());
        break;
      default:
        throw new EnclaveException(
            "Unable to support private key storage of type: " + storedPrivateKey.getType());
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
  public PrivateKey getPrivateKey(PublicKey publicKey) {
    return cache.get(publicKey);
  }

  @Override
  public PublicKey generateKeyPair(KeyConfig config) {
    String basePath = config.getBasePath();
    Optional<String> password = config.getPassword();
    return generateStoreAndCache(basePath, password);
  }

  private Optional<String[]> lookupPasswords() {
    Optional<File> passwords = config.passwords();
    Optional<String[]> passwordList = Optional.empty();
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
    PrivateKeyData data = new PrivateKeyData();

    StoredPrivateKey privateKey = new StoredPrivateKey(data);
    if (password.isPresent()) {
      privateKey.setType(StoredPrivateKey.ARGON2_SBOX);
      ArgonOptions argonOptions = defaultArgonOptions();
      data.setAopts(argonOptions);
      SodiumArgon2Sbox sodiumArgon2Sbox = new SodiumArgon2Sbox(config);

      byte[] snonce = sodiumArgon2Sbox.generateSnonce();
      data.setSnonce(Base64.encode(snonce));
      byte[] asalt = sodiumArgon2Sbox.generateAsalt();
      data.setAsalt(Base64.encode(asalt));
      byte[] encryptKey =
          sodiumArgon2Sbox.encrypt(
              keyPair.getPrivateKey(), password.get(), asalt, snonce, argonOptions);
      data.setSbox(Base64.encode(encryptKey));
    } else {
      data.setBytes(Base64.encode(keyPair.getPrivateKey()));
      privateKey.setType(StoredPrivateKey.UNLOCKED);
    }
    return privateKey;
  }

  @NotNull
  private ArgonOptions defaultArgonOptions() {
    ArgonOptions argonOptions = new ArgonOptions();
    argonOptions.setVariant("i");
    argonOptions.setOpsLimit(ArgonOptions.OPS_LIMIT_MODERATE);
    argonOptions.setMemLimit(ArgonOptions.MEM_LIMIT_MODERATE);
    argonOptions.setVersion(ArgonOptions.VERSION);
    return argonOptions;
  }

  public PublicKey[] alwaysSendTo() {
    File[] alwaysSendTo = config.alwaysSendTo();
    return Arrays.stream(alwaysSendTo).map(file -> readPublicKey(file)).toArray(PublicKey[]::new);
  }

  @Override
  public PublicKey[] nodeKeys() {
    File[] publicKeys = config.publicKeys();
    return Arrays.stream(publicKeys).map(file -> readPublicKey(file)).toArray(PublicKey[]::new);
  }
}
