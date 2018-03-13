package net.consensys.orion.impl.enclave;

import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.enclave.HashAlgorithm;
import net.consensys.orion.api.exception.OrionErrorCode;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

public class Hasher {
  //TODO consider interface/implementation split
  static {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  public byte[] digest(HashAlgorithm algorithm, byte[] input) {
    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(algorithm.getName());
      digest.update(input);
      return digest.digest();
    } catch (final NoSuchAlgorithmException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_UNSUPPORTED_ALGORTHIM, e);
    }
  }
}
