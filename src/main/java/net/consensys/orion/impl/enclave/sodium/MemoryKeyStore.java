package net.consensys.orion.impl.enclave.sodium;

import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.enclave.KeyConfig;
import net.consensys.orion.api.enclave.KeyStore;
import net.consensys.orion.api.enclave.PrivateKey;
import net.consensys.orion.api.enclave.PublicKey;
import net.consensys.orion.api.exception.OrionErrorCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.muquit.libsodiumjna.SodiumKeyPair;
import com.muquit.libsodiumjna.SodiumLibrary;
import com.muquit.libsodiumjna.exceptions.SodiumLibraryException;

public class MemoryKeyStore implements KeyStore {

  private final Map<PublicKey, PrivateKey> store = new HashMap<>();
  private final List<PublicKey> nodeKeys = new ArrayList<>();

  @Override
  public Optional<PrivateKey> privateKey(PublicKey publicKey) {
    return Optional.ofNullable(store.get(publicKey));
  }

  @Override
  public PublicKey generateKeyPair(KeyConfig keyConfig) {
    try {
      final SodiumKeyPair keyPair = SodiumLibrary.cryptoBoxKeyPair();
      final PrivateKey privateKey = new PrivateKey(keyPair.getPrivateKey());
      final PublicKey publicKey = new PublicKey(keyPair.getPublicKey());
      store.put(publicKey, privateKey);
      return publicKey;
    } catch (final SodiumLibraryException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_CREATE_KEY_PAIR, e);
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
