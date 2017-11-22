package net.consensys.athena.impl.enclave;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.enclave.HashAlgorithm;
import net.consensys.athena.impl.enclave.bouncycastle.Hasher;

import java.security.PrivateKey;
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
  public void generateKeyPair(String name) {}

  @Override
  public byte[] decrypt(EncryptedPayload payload, PrivateKey privateKey) {
    return new byte[0];
  }

  @Override
  public EncryptedPayload encrypt(byte[] payload, PublicKey senderKey, PublicKey[] recipients) {
    return null;
  }
}
