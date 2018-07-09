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

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.crypto.sodium.PasswordHash;
import net.consensys.cava.crypto.sodium.PasswordHash.Algorithm;
import net.consensys.cava.crypto.sodium.SecretBox;
import net.consensys.cava.crypto.sodium.SodiumException;
import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.exception.OrionErrorCode;

public class SodiumArgon2Sbox {

  public static Box.SecretKey decrypt(
      byte[] cipherText,
      String password,
      PasswordHash.Salt salt,
      SecretBox.Nonce nonce,
      ArgonOptions argonOptions) {
    long opsLimit = argonOptions.opsLimit().get();
    long memLimit = argonOptions.memLimit().get();
    Algorithm algorithm = lookupAlgorithm(argonOptions);
    byte[] clearText;
    try {
      byte[] pwhash =
          PasswordHash.hash(password.getBytes(UTF_8), SecretBox.Key.length(), salt, opsLimit, memLimit, algorithm);
      SecretBox.Key key = SecretBox.Key.fromBytes(pwhash);
      clearText = SecretBox.decrypt(cipherText, key, nonce);
    } catch (final SodiumException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_STORAGE_DECRYPT, e);
    }
    if (clearText == null) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_STORAGE_DECRYPT, "Key decryption failed");
    }
    return Box.SecretKey.fromBytes(clearText);
  }

  public static byte[] encrypt(
      Box.SecretKey secretKey,
      String password,
      PasswordHash.Salt salt,
      SecretBox.Nonce nonce,
      ArgonOptions argonOptions) {
    long opsLimit = argonOptions.opsLimit().get();
    long memLimit = argonOptions.memLimit().get();
    Algorithm algorithm = lookupAlgorithm(argonOptions);
    try {
      byte[] pwhash =
          PasswordHash.hash(password.getBytes(UTF_8), SecretBox.Key.length(), salt, opsLimit, memLimit, algorithm);
      SecretBox.Key key = SecretBox.Key.fromBytes(pwhash);
      return SecretBox.encrypt(secretKey.bytesArray(), key, nonce);
    } catch (final SodiumException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_STORAGE_ENCRYPT, e);
    }
  }

  private static PasswordHash.Algorithm lookupAlgorithm(ArgonOptions argonOptions) {
    switch (argonOptions.variant()) {
      case "i":
        return PasswordHash.Algorithm.argon2i13();
      case "id":
        return PasswordHash.Algorithm.argon2id13();
      default:
        throw new EnclaveException(
            OrionErrorCode.ENCLAVE_UNSUPPORTED_STORAGE_ALGORITHM,
            "Unsupported variant: " + argonOptions.variant());
    }
  }
}
