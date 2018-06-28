package net.consensys.orion.api.enclave;

import java.util.Optional;

/**
 * Interface for key put. Provides a mechanism for generating keys, and looking up a private key for a given public key.
 * Typically used internally by an enclave to look up private keys.
 */
public interface KeyStore {

  /**
   * Lookup the private key for a given public key.
   *
   * @param publicKey PublicKey to get the private key for.
   * @return Optional Return the public key.
   */
  Optional<PrivateKey> privateKey(PublicKey publicKey);

  /**
   * Generate and put a new keypair, returning the public key for external use.
   *
   * @param keyConfig Configuration for key generation.
   * @return Return the public key part of the key pair.
   */
  PublicKey generateKeyPair(KeyConfig keyConfig);

  PublicKey[] alwaysSendTo();

  PublicKey[] nodeKeys();
}
