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

package net.consensys.orion.enclave;

import net.consensys.cava.crypto.sodium.Box;

public interface Enclave {

  /**
   * Encrypts the plaintext from the sender to the recipients. Creates the encrypted payload that encapsulates the
   * ciphertext and related keying data.
   *
   * @param plaintext Plaintext to encrypt
   * @param senderKey public key of the sender of the message.
   * @param recipients public keys of the recipients.
   * @return Returns a EncryptedPayload that encapsulates the ciphertext and related metadata.
   */
  EncryptedPayload encrypt(byte[] plaintext, Box.PublicKey senderKey, Box.PublicKey[] recipients);

  /**
   * Decrypt the cipher text in the encrypted payload, using the private key associated with the public key identity. It
   * is the responsibility of the enclave to look up the private key, which may be stored securely.
   *
   * @param ciphertextAndMetadata EncryptedPayload containing the ciphertext and related metadata to decrypt.
   * @param identity PublicKey identity that wants to perform the decryption. Will be used to look up the private key to
   *        perform the decryption.
   * @return return the decrypted byte array that is the data.
   */
  byte[] decrypt(EncryptedPayload ciphertextAndMetadata, Box.PublicKey identity);

  Box.PublicKey[] alwaysSendTo();

  Box.PublicKey[] nodeKeys();

  Box.PublicKey readKey(String b64);
}
