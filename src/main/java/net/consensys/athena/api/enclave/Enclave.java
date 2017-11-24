package net.consensys.athena.api.enclave;

import java.security.PrivateKey;
import java.security.PublicKey;

public interface Enclave {
  byte[] digest(HashAlgorithm algorithm, byte[] input);

  // name is the base name of the files that the keypair will be stored in, it will have a .pub
  // suffix for the public key, and a .key suffix for the private key.
  void generateKeyPair(String name);

  EncryptedPayload encrypt(byte[] plaintext, PublicKey senderKey, PublicKey[] recipients);

  byte[] decrypt(EncryptedPayload ciphertextAndMetadata, PrivateKey privateKey);
}
