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

package net.consensys.orion.api.enclave;

import net.consensys.cava.crypto.sodium.Box;

import javax.annotation.Nullable;

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
  @Nullable
  Box.SecretKey privateKey(Box.PublicKey publicKey);

  Box.PublicKey[] alwaysSendTo();

  Box.PublicKey[] nodeKeys();
}
