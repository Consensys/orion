package net.consensys.orion.impl.enclave.sodium;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.crypto.sodium.SodiumException;
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
      Box.KeyPair keyPair = Box.KeyPair.random();
      final PrivateKey privateKey = new PrivateKey(keyPair.secretKey().bytesArray());
      final PublicKey publicKey = new PublicKey(keyPair.publicKey().bytesArray());
      store.put(publicKey, privateKey);
      return publicKey;
    } catch (final SodiumException e) {
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
    return nodeKeys.toArray(new PublicKey[0]);
  }
}
