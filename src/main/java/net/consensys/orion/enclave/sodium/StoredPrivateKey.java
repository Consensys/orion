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
package net.consensys.orion.enclave.sodium;

import net.consensys.orion.enclave.EnclaveException;
import net.consensys.orion.enclave.sodium.serialization.StoredPrivateKeyDeserializer;
import net.consensys.orion.enclave.sodium.serialization.StoredPrivateKeySerializer;
import net.consensys.orion.exception.OrionErrorCode;

import java.util.Arrays;
import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.tuweni.crypto.sodium.Box;
import org.apache.tuweni.crypto.sodium.Box.SecretKey;
import org.apache.tuweni.crypto.sodium.PasswordHash;
import org.apache.tuweni.crypto.sodium.SecretBox;
import org.apache.tuweni.io.Base64;

@JsonSerialize(using = StoredPrivateKeySerializer.class)
@JsonDeserialize(using = StoredPrivateKeyDeserializer.class)
public class StoredPrivateKey {
  public static final String UNLOCKED = "unlocked";
  public static final String ENCRYPTED = "sodium-encrypted";

  // The "moderate" limits - copied here to ensure they remain stable between different sodium library versions
  // Also, to guard against malicious inputs, these limits are not read from the file but instead assumed for all
  // keys of type 'sodium-encrypted'
  private static final int ENCRYPT_OPS_LIMIT = 3;
  private static final int ENCRYPT_MEM_LIMIT = 268435456;
  private static final PasswordHash.Algorithm ENCRYPT_ALGORITHM = PasswordHash.Algorithm.argon2i13();

  private final String encoded;
  private final String type;

  public StoredPrivateKey(final String encoded, final String type) {
    this.encoded = encoded;
    this.type = type;
  }

  public String encoded() {
    return encoded;
  }

  public String type() {
    return type;
  }

  static StoredPrivateKey fromSecretKey(final Box.SecretKey secretKey, @Nullable final String password) {
    if (password == null) {
      return new StoredPrivateKey(Base64.encodeBytes(secretKey.bytesArray()), UNLOCKED);
    }
    return lock(secretKey, password);
  }

  private static StoredPrivateKey lock(final SecretKey secretKey, final String password) {
    final byte[] keyBytes = secretKey.bytesArray();
    try {
      final byte[] cipherText =
          SecretBox.encrypt(keyBytes, password, ENCRYPT_OPS_LIMIT, ENCRYPT_MEM_LIMIT, ENCRYPT_ALGORITHM);
      return new StoredPrivateKey(Base64.encodeBytes(cipherText), ENCRYPTED);
    } finally {
      Arrays.fill(keyBytes, (byte) 0);
    }
  }

  Box.SecretKey toSecretKey(@Nullable final String password) {
    switch (type) {
      case UNLOCKED:
        return Box.SecretKey.fromBytes(Base64.decode(encoded));
      case ENCRYPTED:
        if (password == null) {
          throw new EnclaveException(
              OrionErrorCode.ENCLAVE_MISSING_PRIVATE_KEY_PASSWORD,
              "private key is encrypted and requires a password");
        }
        return unlock(password);
      default:
        throw new EnclaveException(
            OrionErrorCode.ENCLAVE_UNSUPPORTED_PRIVATE_KEY_TYPE,
            "Unable to support private key storage of type '" + type + "'");
    }
  }

  private SecretKey unlock(final String password) {
    final byte[] keyBytes = SecretBox
        .decrypt(Base64.decodeBytes(encoded), password, ENCRYPT_OPS_LIMIT, ENCRYPT_MEM_LIMIT, ENCRYPT_ALGORITHM);
    if (keyBytes == null) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_STORAGE_DECRYPT, "Key decryption failed");
    }
    try {
      return SecretKey.fromBytes(keyBytes);
    } finally {
      Arrays.fill(keyBytes, (byte) 0);
    }
  }
}
