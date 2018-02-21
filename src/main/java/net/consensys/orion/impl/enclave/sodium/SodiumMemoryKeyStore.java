package net.consensys.orion.impl.enclave.sodium;

import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.enclave.KeyConfig;
import net.consensys.orion.api.enclave.KeyStore;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.muquit.libsodiumjna.SodiumKeyPair;
import com.muquit.libsodiumjna.SodiumLibrary;
import com.muquit.libsodiumjna.exceptions.SodiumLibraryException;

public class SodiumMemoryKeyStore implements KeyStore {

  private Map<PublicKey, PrivateKey> store = new HashMap<>();
  private List<PublicKey> nodeKeys = new ArrayList<>();

  public SodiumMemoryKeyStore(Config config) {
    SodiumLibrary.setLibraryPath(config.libSodiumPath());
  }

  @Override
  public PrivateKey privateKey(PublicKey publicKey) {
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

  public void addNodeKey(PublicKey key) {
    nodeKeys.add(key);
  }

  @Override
  public PublicKey[] nodeKeys() {
    return nodeKeys.toArray(new PublicKey[nodeKeys.size()]);
  }
}
