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

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.crypto.sodium.Box.PublicKey;
import net.consensys.cava.crypto.sodium.SodiumException;
import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.enclave.KeyStore;
import net.consensys.orion.api.exception.OrionErrorCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class MemoryKeyStore implements KeyStore {

  private final Map<Box.PublicKey, Box.SecretKey> store = new HashMap<>();
  private final List<Box.PublicKey> nodeKeys = new ArrayList<>();

  @Override
  @Nullable
  public Box.SecretKey privateKey(Box.PublicKey publicKey) {
    return store.get(publicKey);
  }

  /**
   * Generate and put a new keypair, returning the public key for external use.
   *
   * @return Return the public key part of the key pair.
   */
  public PublicKey generateKeyPair() {
    try {
      Box.KeyPair keyPair = Box.KeyPair.random();
      Box.PublicKey publicKey = keyPair.publicKey();
      store.put(publicKey, keyPair.secretKey());
      return publicKey;
    } catch (final SodiumException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_CREATE_KEY_PAIR, e);
    }
  }

  @Override
  public Box.PublicKey[] alwaysSendTo() {
    return new Box.PublicKey[0];
  }

  public void addNodeKey(Box.PublicKey key) {
    nodeKeys.add(key);
  }

  @Override
  public Box.PublicKey[] nodeKeys() {
    return nodeKeys.toArray(new Box.PublicKey[0]);
  }
}
