package net.consensys.athena.api.enclave;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Interface for key put. Provides a mechanism for generating keys, and looking up a private key for
 * a given public key. Typically used internally by an enclave to look up private keys.
 */
public interface KeyStore {

  /**
   * Lookup the private key for a given public key.
   *
   * @param publicKey PublicKey to get the private key for.
   * @return Return the public key.
   */
  PrivateKey getPrivateKey(PublicKey publicKey);

  /**
   * Generate and put a new keypair, returning the public key for external use.
   *
   * @return Return the public key part of the key pair.
   */
  PublicKey generateKeyPair();

  PublicKey[] alwaysSendTo();

  PublicKey[] nodeKeys();
}
