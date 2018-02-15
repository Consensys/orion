package net.consensys.athena.impl.enclave.sodium;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.api.enclave.CombinedKey;
import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EnclaveException;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.enclave.HashAlgorithm;
import net.consensys.athena.api.enclave.KeyStore;
import net.consensys.athena.impl.enclave.Hasher;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;

import com.muquit.libsodiumjna.SodiumLibrary;
import com.muquit.libsodiumjna.exceptions.SodiumLibraryException;

public class LibSodiumEnclave implements Enclave {

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
      if (!(senderKey instanceof SodiumPublicKey)) {
        throw new EnclaveException("SodiumEnclave needs SodiumPublicKey");
      }
      PrivateKey senderPrivateKey = keyStore.privateKey(senderKey);
      if (senderPrivateKey == null) {
        throw new EnclaveException("No StoredPrivateKey found in keystore");
      }
      byte[] secretKey =
          SodiumLibrary.randomBytes(SodiumLibrary.cryptoSecretBoxKeyBytes().intValue());
      byte[] secretNonce = secretNonce();
      byte[] cipherText = SodiumLibrary.cryptoSecretBoxEasy(plaintext, secretNonce, secretKey);

      byte[] nonce = nonce();
      SodiumCombinedKey[] combinedKeys =
          combinedKeys(recipients, senderPrivateKey, secretKey, nonce);

      // store mapping between combined keys and recipients
      return new SodiumEncryptedPayload(
          (SodiumPublicKey) senderKey,
          secretNonce,
          nonce,
          combinedKeys,
          cipherText,
          Optional.of(combinedKeysMapping(recipients)));
    } catch (SodiumLibraryException e) {
      throw new EnclaveException(e);
    }
  }

  @Override
  public byte[] decrypt(EncryptedPayload ciphertextAndMetadata, PublicKey identity) {
    try {
      PrivateKey privateKey = keyStore.privateKey(identity);
      if (privateKey == null) {
        throw new EnclaveException("No StoredPrivateKey found in keystore");
      }
      CombinedKey key = ciphertextAndMetadata.combinedKeys()[0];
      byte[] secretKey =
          SodiumLibrary.cryptoBoxOpenEasy(
              key.getEncoded(),
              ciphertextAndMetadata.combinedKeyNonce(),
              ciphertextAndMetadata.sender().getEncoded(),
              privateKey.getEncoded());
      return SodiumLibrary.cryptoSecretBoxOpenEasy(
          ciphertextAndMetadata.cipherText(), ciphertextAndMetadata.nonce(), secretKey);
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

  private HashMap<SodiumPublicKey, Integer> combinedKeysMapping(PublicKey[] recipients) {
    HashMap<SodiumPublicKey, Integer> combinedKeysMapping = new HashMap<>();
    for (int i = 0; i < recipients.length; i++) {
      combinedKeysMapping.put((SodiumPublicKey) recipients[i], i);
    }
    return combinedKeysMapping;
  }

  private SodiumCombinedKey[] combinedKeys(
      PublicKey[] recipients, PrivateKey senderPrivateKey, byte[] secretKey, byte[] nonce)
      throws SodiumLibraryException {
    SodiumCombinedKey[] combinedKeys = new SodiumCombinedKey[recipients.length];
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
