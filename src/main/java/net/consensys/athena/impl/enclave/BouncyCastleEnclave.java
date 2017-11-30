package net.consensys.athena.impl.enclave;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.enclave.HashAlgorithm;
import net.consensys.athena.impl.enclave.bouncycastle.Hasher;

import java.security.PublicKey;
import java.security.Security;

public class BouncyCastleEnclave implements Enclave {
  static {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  private final Hasher hasher = new Hasher();

  @Override
  public byte[] digest(HashAlgorithm algorithm, byte[] input) {
    return hasher.digest(algorithm, input);
  }

  @Override
  public EncryptedPayload encrypt(byte[] plaintext, PublicKey senderKey, PublicKey[] recipients) {
    return null;
  }

  @Override
  public byte[] decrypt(EncryptedPayload ciphertextAndMetadata, PublicKey identity) {
    return new byte[0];
  }
}
