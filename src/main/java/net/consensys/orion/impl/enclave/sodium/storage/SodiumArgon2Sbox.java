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

package net.consensys.orion.impl.enclave.sodium.storage;

import static java.nio.charset.StandardCharsets.UTF_8;

import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.impl.utils.Base64;

import com.muquit.libsodiumjna.SodiumLibrary;
import com.muquit.libsodiumjna.exceptions.SodiumLibraryException;
import com.sun.jna.NativeLong;

public class SodiumArgon2Sbox {

  public byte[] decrypt(StoredPrivateKey storedPrivateKey, String password) {
    final ArgonOptions argonOptions = storedPrivateKey.data().aopts().get();
    final String asalt = storedPrivateKey.data().asalt().get();
    final int algorithm = lookupAlgorithm(argonOptions);
    try {
      final byte[] pwhash = SodiumLibrary.cryptoPwhash(
          password.getBytes(UTF_8),
          decode(asalt),
          argonOptions.opsLimit().get(),
          new NativeLong(argonOptions.memLimit().get()),
          algorithm);
      return SodiumLibrary.cryptoSecretBoxOpenEasy(
          decode(storedPrivateKey.data().sbox().get()),
          decode(storedPrivateKey.data().snonce().get()),
          pwhash);
    } catch (final SodiumLibraryException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_STORAGE_DECRYPT, e);
    }
  }

  private byte[] decode(String asalt) {
    return Base64.decode(asalt);
  }

  private int lookupAlgorithm(ArgonOptions argonOptions) {
    switch (argonOptions.variant()) {
      case "i":
        return SodiumLibrary.cryptoPwhashAlgArgon2i13();
      case "id":
        return SodiumLibrary.cryptoPwhashAlgArgon2id13();
      default:
        throw new EnclaveException(
            OrionErrorCode.ENCLAVE_UNSUPPORTED_STORAGE_ALGORITHM,
            "Unsupported variant: " + argonOptions.variant());
    }
  }

  public byte[] generateAsalt() {
    return SodiumLibrary.randomBytes(SodiumLibrary.cryptoPwhashSaltBytes());
  }

  public byte[] generateSnonce() {
    return SodiumLibrary.randomBytes(SodiumLibrary.cryptoSecretBoxNonceBytes().intValue());
  }

  public byte[] encrypt(
      final byte[] privateKey,
      String password,
      byte[] asalt,
      byte[] snonce,
      ArgonOptions argonOptions) {
    try {
      final byte[] pwhash = SodiumLibrary.cryptoPwhash(
          password.getBytes(UTF_8),
          asalt,
          argonOptions.opsLimit().get(),
          new NativeLong(argonOptions.memLimit().get()),
          lookupAlgorithm(argonOptions));
      return SodiumLibrary.cryptoSecretBoxEasy(privateKey, snonce, pwhash);

    } catch (final SodiumLibraryException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_STORAGE_ENCRYPT, e);
    }
  }
}
