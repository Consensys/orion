package net.consensys.orion.impl.enclave.sodium;

import net.consensys.orion.api.enclave.CombinedKey;
import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.enclave.KeyStore;
import net.consensys.orion.api.enclave.PrivateKey;
import net.consensys.orion.api.enclave.PublicKey;
import net.consensys.orion.api.exception.OrionErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;

import com.muquit.libsodiumjna.SodiumLibrary;
import com.muquit.libsodiumjna.exceptions.SodiumLibraryException;

public class LibSodiumEnclave implements Enclave {

  private KeyStore keyStore;

  private final PublicKey[] alwaysSendTo;
  private final PublicKey[] nodeKeys;

  public LibSodiumEnclave(KeyStore keyStore) {
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
  public EncryptedPayload encrypt(byte[] plaintext, PublicKey senderKey, PublicKey[] recipients) {
    final PublicKey[] recipientsAndSender = addSenderToRecipients(recipients, senderKey);
    final PrivateKey senderPrivateKey = privateKey(senderKey);
    final byte[] secretKey = secretKey();
    final byte[] secretNonce = secretNonce();
    final byte[] cipherText = encrypt(plaintext, secretNonce, secretKey);
    final byte[] nonce = nonce();
    final CombinedKey[] combinedKeys = combinedKeys(recipientsAndSender, senderPrivateKey, secretKey, nonce);

    return new EncryptedPayload(
        senderKey,
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
      return new PublicKey(Base64.getDecoder().decode(b64.getBytes(StandardCharsets.UTF_8)));

    } catch (final IllegalArgumentException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_DECODE_PUBLIC_KEY, e);
    }
  }

  private PublicKey[] addSenderToRecipients(final PublicKey[] recipients, final PublicKey sender) {
    final PublicKey[] recipientsAndSender = Arrays.copyOf(recipients, recipients.length + 1);
    recipientsAndSender[recipients.length] = sender;
    return recipientsAndSender;
  }

  private byte[] decrypt(EncryptedPayload ciphertextAndMetadata, byte[] secretKey) {
    try {
      return SodiumLibrary
          .cryptoSecretBoxOpenEasy(ciphertextAndMetadata.cipherText(), ciphertextAndMetadata.nonce(), secretKey);
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
            ciphertextAndMetadata.sender().toBytes(),
            privateKey.toBytes());

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
    final Optional<PrivateKey> privateKey = keyStore.privateKey(identity);
    if (!privateKey.isPresent()) {
      throw new EnclaveException(
          OrionErrorCode.ENCLAVE_NO_MATCHING_PRIVATE_KEY,
          "No StoredPrivateKey found in keystore");
    }

    return privateKey.get();
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
  private HashMap<PublicKey, Integer> combinedKeysMapping(PublicKey[] recipients) {
    final HashMap<PublicKey, Integer> combinedKeysMapping = new HashMap<>();
    for (int i = 0; i < recipients.length; i++) {
      combinedKeysMapping.put(recipients[i], i);
    }

    return combinedKeysMapping;
  }

  private CombinedKey[] combinedKeys(
      PublicKey[] recipients,
      PrivateKey senderPrivateKey,
      byte[] secretKey,
      byte[] nonce) {

    try {
      final CombinedKey[] combinedKeys = new CombinedKey[recipients.length];
      for (int i = 0; i < recipients.length; i++) {
        final PublicKey recipient = recipients[i];
        final byte[] encryptedKey =
            SodiumLibrary.cryptoBoxEasy(secretKey, nonce, recipient.toBytes(), senderPrivateKey.toBytes());
        final CombinedKey combinedKey = new CombinedKey(encryptedKey);
        combinedKeys[i] = combinedKey;
      }

      return combinedKeys;
    } catch (final SodiumLibraryException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_ENCRYPT_COMBINE_KEYS, e);
    }
  }
}
