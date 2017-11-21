package net.consensys.athena.api.enclave;

import net.consensys.athena.api.enclave.bouncycastle.Hasher;

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
}
