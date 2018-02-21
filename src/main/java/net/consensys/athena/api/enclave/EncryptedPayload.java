package net.consensys.athena.api.enclave;

import java.security.PublicKey;

public interface EncryptedPayload {
  PublicKey sender();

  byte[] cipherText();

  byte[] nonce();

  CombinedKey[] combinedKeys();

  byte[] combinedKeyNonce();

  EncryptedPayload stripFor(PublicKey key);
}
