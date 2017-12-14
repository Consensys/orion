package net.consensys.athena.impl.enclave.sodium;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.api.enclave.CombinedKey;
import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EnclaveException;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.enclave.HashAlgorithm;
import net.consensys.athena.api.enclave.KeyStore;
import net.consensys.athena.impl.enclave.SimpleEncryptedPayload;
import net.consensys.athena.impl.enclave.bouncycastle.Hasher;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;

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

  public LibSodiumEnclave(Config config, KeyStore keyStore) {
    SodiumLibrary.setLibraryPath(config.libSodiumPath());

    this.keyStore = keyStore;
  }

  @Override
  public byte[] digest(HashAlgorithm algorithm, byte[] input) {
    return hasher.digest(algorithm, input);
  }

  @Override
  public EncryptedPayload encrypt(byte[] plaintext, PublicKey senderKey, PublicKey[] recipients) {
    try {
      PrivateKey senderPrivateKey = keyStore.getPrivateKey(senderKey);
      byte[] secretKey =
          SodiumLibrary.randomBytes(SodiumLibrary.cryptoSecretBoxKeyBytes().intValue());
      byte[] secretNonce = secretNonce();
      byte[] cipherText = SodiumLibrary.cryptoSecretBoxEasy(plaintext, secretNonce, secretKey);

      byte[] nonce = nonce();
      CombinedKey[] combinedKeys = getCombinedKeys(recipients, senderPrivateKey, secretKey, nonce);
      return new SimpleEncryptedPayload(senderKey, secretNonce, nonce, combinedKeys, cipherText);
    } catch (SodiumLibraryException e) {
      throw new EnclaveException(e);
    }
  }

  @Override
  public byte[] decrypt(EncryptedPayload ciphertextAndMetadata, PublicKey identity) {
    try {
      CombinedKey key = ciphertextAndMetadata.getCombinedKeys()[0];
      PrivateKey privateKey = keyStore.getPrivateKey(identity);
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
