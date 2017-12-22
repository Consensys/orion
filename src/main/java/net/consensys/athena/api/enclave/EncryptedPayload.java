package net.consensys.athena.api.enclave;

import java.security.PublicKey;

public interface EncryptedPayload {
  PublicKey getSender();

  byte[] getCipherText();

  byte[] getNonce();

  CombinedKey[] getCombinedKeys();

  byte[] getCombinedKeyNonce();

  EncryptedPayload stripFor(PublicKey key);
}
