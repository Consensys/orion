/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package net.consensys.orion.impl.enclave.sodium;

import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.enclave.KeyConfig;
import net.consensys.orion.api.enclave.KeyStore;
import net.consensys.orion.api.exception.OrionErrorCode;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.muquit.libsodiumjna.SodiumKeyPair;
import com.muquit.libsodiumjna.SodiumLibrary;
import com.muquit.libsodiumjna.exceptions.SodiumLibraryException;

public class SodiumMemoryKeyStore implements KeyStore {

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
      final SodiumPrivateKey privateKey = new SodiumPrivateKey(keyPair.getPrivateKey());
      final SodiumPublicKey publicKey = new SodiumPublicKey(keyPair.getPublicKey());
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
