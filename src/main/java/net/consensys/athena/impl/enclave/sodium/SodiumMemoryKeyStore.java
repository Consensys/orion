package net.consensys.athena.impl.enclave.sodium;

import net.consensys.athena.api.enclave.EnclaveException;
import net.consensys.athena.api.enclave.KeyConfig;
import net.consensys.athena.api.enclave.KeyStore;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import com.muquit.libsodiumjna.SodiumKeyPair;
import com.muquit.libsodiumjna.SodiumLibrary;
import com.muquit.libsodiumjna.exceptions.SodiumLibraryException;

public class SodiumMemoryKeyStore implements KeyStore {

  Map<PublicKey, PrivateKey> store = new HashMap<>();

  @Override
  public PrivateKey getPrivateKey(PublicKey publicKey) {
    return store.get(publicKey);
  }

  @Override
  public PublicKey generateKeyPair(KeyConfig keyConfig) {
    try {
      SodiumKeyPair keyPair = SodiumLibrary.cryptoBoxKeyPair();
      SodiumPrivateKey privateKey = new SodiumPrivateKey(keyPair.getPrivateKey());
      SodiumPublicKey publicKey = new SodiumPublicKey(keyPair.getPublicKey());
      store.put(publicKey, privateKey);
      return publicKey;
    } catch (SodiumLibraryException e) {
      throw new EnclaveException(e);
    }
  }

  @Override
  public PublicKey[] alwaysSendTo() {
    return new PublicKey[0];
  }
}
