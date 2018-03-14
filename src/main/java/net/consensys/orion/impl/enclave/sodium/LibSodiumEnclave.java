package net.consensys.orion.impl.enclave.sodium;

import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.enclave.CombinedKey;
import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.enclave.HashAlgorithm;
import net.consensys.orion.api.enclave.KeyStore;
import net.consensys.orion.api.exception.OrionErrorCode;
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
    final byte[] secretKey = secretKey();
    final byte[] secretNonce = secretNonce();
    final byte[] cipherText = encrypt(plaintext, secretNonce, secretKey);
    final byte[] nonce = nonce();
    final SodiumCombinedKey[] combinedKeys =
        combinedKeys(recipientsAndSender, senderPrivateKey, secretKey, nonce);

    return new SodiumEncryptedPayload(
        senderPublicKey,
        secretNonce,
        nonce,
        combinedKeys,
        cipherText,
        Optional.of(combinedKeysMapping(recipientsAndSender)));
  }

  @Override
  public byte[] decrypt(EncryptedPayload ciphertextAndMetadata, PublicKey identity) {
    final PrivateKey privateKey = privateKey(identity);
    final byte[] secretKey = secretKey(ciphertextAndMetadata, privateKey);
    return decrypt(ciphertextAndMetadata, secretKey);
  }

  @Override
  public PublicKey readKey(String b64) {
    try {
      return new SodiumPublicKey(Base64.getDecoder().decode(b64.getBytes(StandardCharsets.UTF_8)));

    } catch (final IllegalArgumentException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_DECODE_PUBLIC_KEY, e);
    }
  }

  private SodiumPublicKey sodiumPublicKey(PublicKey senderKey) {
    if (senderKey instanceof SodiumPublicKey) {
      return (SodiumPublicKey) senderKey;
    }

    throw new EnclaveException(
        OrionErrorCode.ENCLAVE_UNSUPPORTED_PUBLIC_KEY_TYPE, "SodiumEnclave needs SodiumPublicKey");
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
    } catch (final SodiumLibraryException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_DECRYPT, e);
    }
  }

  /** Handles multiple candidates for applying the private key to for decryption. */
  private byte[] secretKey(EncryptedPayload ciphertextAndMetadata, PrivateKey privateKey) {
    SodiumLibraryException problem = null;

    // Try each key until we find one that successfully decrypts or we run out of keys
    for (final CombinedKey key : ciphertextAndMetadata.combinedKeys()) {
      try {
        // When decryption with the combined fails, SodiumLibrary exceptions
        return SodiumLibrary.cryptoBoxOpenEasy(
            key.getEncoded(),
            ciphertextAndMetadata.combinedKeyNonce(),
            ciphertextAndMetadata.sender().getEncoded(),
            privateKey.getEncoded());

      } catch (final SodiumLibraryException e) {
        // The next next key might be the lucky one, so don't propagate just yet
        problem = e;
      }
    }

    // No more keys left to try, finally propagate the issue
    throw new EnclaveException(OrionErrorCode.ENCLAVE_DECRYPT_WRONG_PRIVATE_KEY, problem);
  }

  private byte[] nonce() {
    final int nonceBytesLength = SodiumLibrary.cryptoBoxNonceBytes().intValue();
    return SodiumLibrary.randomBytes(nonceBytesLength);
  }

  private PrivateKey privateKey(PublicKey identity) {
    final PrivateKey privateKey = keyStore.privateKey(identity);
    if (privateKey == null) {
      throw new EnclaveException(
          OrionErrorCode.ENCLAVE_NO_MATCHING_PRIVATE_KEY, "No StoredPrivateKey found in keystore");
    }

    return privateKey;
  }

  private byte[] secretNonce() {
    final int secretNonceBytesLength = SodiumLibrary.cryptoSecretBoxNonceBytes().intValue();

    return SodiumLibrary.randomBytes(secretNonceBytesLength);
  }

  private byte[] secretKey() {
    return SodiumLibrary.randomBytes(SodiumLibrary.cryptoSecretBoxKeyBytes().intValue());
  }

  private byte[] encrypt(byte[] plaintext, byte[] secretNonce, byte[] secretKey) {
    try {
      return SodiumLibrary.cryptoSecretBoxEasy(plaintext, secretNonce, secretKey);
    } catch (final SodiumLibraryException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_ENCRYPT, e);
    }
  }

  /** Create mapping between combined keys and recipients */
  private HashMap<SodiumPublicKey, Integer> combinedKeysMapping(PublicKey[] recipients) {
    final HashMap<SodiumPublicKey, Integer> combinedKeysMapping = new HashMap<>();
    for (int i = 0; i < recipients.length; i++) {
      combinedKeysMapping.put((SodiumPublicKey) recipients[i], i);
    }

    return combinedKeysMapping;
  }

  private SodiumCombinedKey[] combinedKeys(
      PublicKey[] recipients, PrivateKey senderPrivateKey, byte[] secretKey, byte[] nonce) {

    try {
      final SodiumCombinedKey[] combinedKeys = new SodiumCombinedKey[recipients.length];
      for (int i = 0; i < recipients.length; i++) {
        final PublicKey recipient = recipients[i];
        final byte[] encryptedKey =
            SodiumLibrary.cryptoBoxEasy(
                secretKey, nonce, recipient.getEncoded(), senderPrivateKey.getEncoded());
        final SodiumCombinedKey combinedKey = new SodiumCombinedKey(encryptedKey);
        combinedKeys[i] = combinedKey;
      }

      return combinedKeys;
    } catch (final SodiumLibraryException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_ENCRYPT_COMBINE_KEYS, e);
    }
  }
}
