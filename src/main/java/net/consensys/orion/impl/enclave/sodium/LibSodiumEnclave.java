package net.consensys.orion.impl.enclave.sodium;

import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.enclave.CombinedKey;
import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.enclave.HashAlgorithm;
import net.consensys.orion.api.enclave.KeyStore;
import net.consensys.orion.impl.enclave.Hasher;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.Arrays;
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
    final PublicKey[] recipientsAndSender = addSenderToRecipients(recipients, senderKey);
    final SodiumPublicKey senderPublicKey = sodiumPublicKey(senderKey);
    final PrivateKey senderPrivateKey = privateKey(senderKey);

    try {
      final byte[] secretKey =
          SodiumLibrary.randomBytes(SodiumLibrary.cryptoSecretBoxKeyBytes().intValue());
      final byte[] secretNonce = secretNonce();
      final byte[] cipherText =
          SodiumLibrary.cryptoSecretBoxEasy(plaintext, secretNonce, secretKey);
      final byte[] nonce = nonce();
      SodiumCombinedKey[] combinedKeys =
          combinedKeys(recipientsAndSender, senderPrivateKey, secretKey, nonce);

      // store mapping between combined keys and recipients
      return new SodiumEncryptedPayload(
          senderPublicKey,
          secretNonce,
          nonce,
          combinedKeys,
          cipherText,
          Optional.of(combinedKeysMapping(recipientsAndSender)));
    } catch (SodiumLibraryException e) {
      throw new EnclaveException(e);
    }
  }

  @Override
  public byte[] decrypt(EncryptedPayload ciphertextAndMetadata, PublicKey identity) {
    final PrivateKey privateKey = privateKey(identity);
    final byte[] secretKey = secretKey(ciphertextAndMetadata, privateKey);
    return decrypt(ciphertextAndMetadata, secretKey);
  }

  @Override
  public PublicKey readKey(String b64) {
    return new SodiumPublicKey(Base64.getDecoder().decode(b64.getBytes(StandardCharsets.UTF_8)));
  }

  private SodiumPublicKey sodiumPublicKey(PublicKey senderKey) {
    if (senderKey instanceof SodiumPublicKey) {
      return (SodiumPublicKey) senderKey;
    }

    throw new EnclaveException("SodiumEnclave needs SodiumPublicKey");
  }

  private PublicKey[] addSenderToRecipients(final PublicKey[] recipients, final PublicKey sender) {
    final PublicKey[] recipientsAndSender = Arrays.copyOf(recipients, recipients.length + 1);
    recipientsAndSender[recipients.length] = sender;
    return recipientsAndSender;
  }

  private byte[] decrypt(EncryptedPayload ciphertextAndMetadata, byte[] secretKey) {
    try {
      return SodiumLibrary.cryptoSecretBoxOpenEasy(
          ciphertextAndMetadata.cipherText(), ciphertextAndMetadata.nonce(), secretKey);
    } catch (SodiumLibraryException e) {
      throw new EnclaveException(e);
    }
  }

  /** Handles multiple candidates for applying the private key to for decryption. */
  private byte[] secretKey(EncryptedPayload ciphertextAndMetadata, PrivateKey privateKey) {
    SodiumLibraryException problem = null;
    byte[] secretKey = null;

    for (final CombinedKey key : ciphertextAndMetadata.combinedKeys()) {
      try {
        // When decryption with the combined fails, SodiumLibrary exceptions
        secretKey =
            SodiumLibrary.cryptoBoxOpenEasy(
                key.getEncoded(),
                ciphertextAndMetadata.combinedKeyNonce(),
                ciphertextAndMetadata.sender().getEncoded(),
                privateKey.getEncoded());

        // Getting to here mean decryption success (valid combined key)
        break;
      } catch (final SodiumLibraryException e) {
        problem = e;
      }
    }

    if (secretKey == null) {
      throw new EnclaveException(problem);
    }

    return secretKey;
  }

  private PrivateKey privateKey(PublicKey identity) {

    final PrivateKey privateKey = keyStore.privateKey(identity);
    if (privateKey == null) {
      throw new EnclaveException("No StoredPrivateKey found in keystore");
    }

    return privateKey;
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
