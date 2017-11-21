package net.consensys.athena.api.enclave;

public interface Enclave {
  byte[] digest(HashAlgorithm algorithm, byte[] input);
}
