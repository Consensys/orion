package net.consensys.athena.impl.enclave.bouncycastle;

import net.consensys.athena.api.enclave.HashAlgorithm;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hasher {

  public byte[] digest(HashAlgorithm algorithm, byte[] input) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(algorithm.getName());
      digest.update(input);
      return digest.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
