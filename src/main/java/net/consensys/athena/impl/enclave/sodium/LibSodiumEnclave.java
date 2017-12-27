package net.consensys.athena.impl.enclave.sodium;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.api.enclave.CombinedKey;
import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EnclaveException;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.enclave.HashAlgorithm;
import net.consensys.athena.api.enclave.KeyStore;
import net.consensys.athena.impl.enclave.Hasher;
import net.consensys.athena.impl.enclave.SimpleEncryptedPayload;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.Base64;
import java.util.HashMap;

import com.muquit.libsodiumjna.SodiumLibrary;
import com.muquit.libsodiumjna.exceptions.SodiumLibraryException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class LibSodiumEnclave implements Enclave {
  private static final Logger log = LogManager.getLogger();

  static {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  private final Hasher hasher = new Hasher();
  private KeyStore keyStore;

  private final PublicKey[] alwaysSendTo;
  private final PublicKey[] nodeKeys;

  public LibSodiumEnclave(Config config, KeyStore keyStore) {
    SodiumLibrary.setLibraryPath(config.libSodiumPath());
    this.keyStore = keyStore;
    this.alwaysSendTo = keyStore.alwaysSendTo();
    this.nodeKeys = keyStore.nodeKeys();
  }

  @Override
  public PublicKey[] alwaysSendTo() {
    return alwaysSendTo;
  }

  @Override
  public PublicKey[] nodeKeys() {
    return nodeKeys;
  }

  @Override
  public byte[] digest(HashAlgorithm algorithm, byte[] input) {
    return hasher.digest(algorithm, input);
  }

  @Override
  public EncryptedPayload encrypt(byte[] plaintext, PublicKey senderKey, PublicKey[] recipients) {
    try {
      PrivateKey senderPrivateKey = keyStore.getPrivateKey(senderKey);
      if (senderPrivateKey == null) {
        throw new EnclaveException("No StoredPrivateKey found in keystore");
      }
      byte[] secretKey =
          SodiumLibrary.randomBytes(SodiumLibrary.cryptoSecretBoxKeyBytes().intValue());
      byte[] secretNonce = secretNonce();
      byte[] cipherText = SodiumLibrary.cryptoSecretBoxEasy(plaintext, secretNonce, secretKey);

      byte[] nonce = nonce();
      CombinedKey[] combinedKeys = getCombinedKeys(recipients, senderPrivateKey, secretKey, nonce);

      // store mapping between combined keys and recipients
      HashMap<PublicKey, Integer> combinedKeysMapping = new HashMap<>();
      for (int i = 0; i < recipients.length; i++) {
        combinedKeysMapping.put(recipients[i], i);
      }
      return new SimpleEncryptedPayload(
          senderKey, secretNonce, nonce, combinedKeys, cipherText, combinedKeysMapping);
    } catch (SodiumLibraryException e) {
      throw new EnclaveException(e);
    }
  }

  @Override
  public byte[] decrypt(EncryptedPayload ciphertextAndMetadata, PublicKey identity) {
    try {
      PrivateKey privateKey = keyStore.getPrivateKey(identity);
      if (privateKey == null) {
        throw new EnclaveException("No StoredPrivateKey found in keystore");
      }
      CombinedKey key = ciphertextAndMetadata.getCombinedKeys()[0];
      byte[] secretKey =
          SodiumLibrary.cryptoBoxOpenEasy(
              key.getEncoded(),
              ciphertextAndMetadata.getCombinedKeyNonce(),
              ciphertextAndMetadata.getSender().getEncoded(),
              privateKey.getEncoded());
      return SodiumLibrary.cryptoSecretBoxOpenEasy(
          ciphertextAndMetadata.getCipherText(), ciphertextAndMetadata.getNonce(), secretKey);
    } catch (SodiumLibraryException e) {
      throw new EnclaveException(e);
    }
  }

  @Override
  public PublicKey readKey(String b64) {
    return new SodiumPublicKey(Base64.getDecoder().decode(b64.getBytes(StandardCharsets.UTF_8)));
  }

  private byte[] secretNonce() {
    int secretNonceBytesLength = SodiumLibrary.cryptoSecretBoxNonceBytes().intValue();

    return SodiumLibrary.randomBytes(secretNonceBytesLength);
  }

  private byte[] nonce() {
    int nonceBytesLength = SodiumLibrary.cryptoBoxNonceBytes().intValue();
    return SodiumLibrary.randomBytes(nonceBytesLength);
  }

  @NotNull
  private CombinedKey[] getCombinedKeys(
      PublicKey[] recipients, PrivateKey senderPrivateKey, byte[] secretKey, byte[] nonce)
      throws SodiumLibraryException {
    CombinedKey[] combinedKeys = new CombinedKey[recipients.length];
    for (int i = 0; i < recipients.length; i++) {
      PublicKey recipient = recipients[i];
      byte[] encryptedKey =
          SodiumLibrary.cryptoBoxEasy(
              secretKey, nonce, recipient.getEncoded(), senderPrivateKey.getEncoded());
      SodiumCombinedKey combinedKey = new SodiumCombinedKey(encryptedKey);
      combinedKeys[i] = combinedKey;
    }
    return combinedKeys;
  }
}
