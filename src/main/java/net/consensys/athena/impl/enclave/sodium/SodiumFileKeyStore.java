package net.consensys.athena.impl.enclave.sodium;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.api.enclave.EnclaveException;
import net.consensys.athena.api.enclave.KeyStore;
import net.consensys.athena.impl.enclave.sodium.storage.StoredPrivateKey;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SodiumFileKeyStore implements KeyStore {

  private Config config;
  private ObjectMapper objectMapper;

  private final Map<PublicKey, PrivateKey> cache = new HashMap<>();

  public SodiumFileKeyStore(Config config, ObjectMapper objectMapper) {
    this.config = config;
    this.objectMapper = objectMapper;

    // load keys
    loadKeysFromConfig(config);
  }

  private void loadKeysFromConfig(Config config) {

    for (int i = 0; i < config.publicKeys().length; i++) {
      File publicKeyFile = config.publicKeys()[i];
      File privateKeyFile = config.privateKeys()[i];
      PublicKey publicKey = readPublicKey(publicKeyFile);
      PrivateKey privateKey = readPrivateKey(privateKeyFile);
      cache.put(publicKey, privateKey);
    }
  }

  private PrivateKey readPrivateKey(File privateKeyFile) {
    try {
      StoredPrivateKey storedPrivateKey =
          objectMapper.readValue(privateKeyFile, StoredPrivateKey.class);
      // TODO decrypt stored key
      if (storedPrivateKey.getType().equals(StoredPrivateKey.UNLOCKED)) {
        byte[] decoded = Base64.getDecoder().decode(storedPrivateKey.getData().getBytes());
        return new SodiumPrivateKey(decoded);
      } else {
        throw new EnclaveException(
            "Unable to support private key storage of type: " + storedPrivateKey.getType());
      }
    } catch (IOException e) {
      throw new EnclaveException(e);
    }
  }

  private PublicKey readPublicKey(File publicKeyFile) {
    try (BufferedReader br = new BufferedReader(new FileReader(publicKeyFile))) {
      String base64Encoded = br.readLine();
      byte[] decoded = Base64.getDecoder().decode(base64Encoded);
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
  public PublicKey generateKeyPair() {
    Optional<String[]> keys = config.generateKeys();
    if (!keys.isPresent()) {
      throw new EnclaveException(
          "Unable to generate key pair as no generatekeys configuration was provided");
    }
    return null;
  }

  public PublicKey[] alwaysSendTo() {
    File[] alwaysSendTo = config.alwaysSendTo();
    return Arrays.stream(alwaysSendTo).map(file -> readPublicKey(file)).toArray(PublicKey[]::new);
  }
}
