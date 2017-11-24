package net.consensys.athena.api.enclave;

import java.security.PublicKey;

public interface EncryptedPayload {
  PublicKey getSender();

  byte[] getCipherText();

  long getNonce();

  CombinedKey[] getCombinedKeys();

  long getCombinedKeyNonce();
}
