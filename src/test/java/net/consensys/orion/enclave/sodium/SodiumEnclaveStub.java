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

import net.consensys.orion.enclave.Enclave;
import net.consensys.orion.enclave.EncryptedKey;
import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.enclave.PrivacyGroupPayload;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.tuweni.crypto.sodium.Box;

public class SodiumEnclaveStub implements Enclave {

  @Override
  public Box.PublicKey[] alwaysSendTo() {
    return new Box.PublicKey[0];
  }

  @Override
  public Box.PublicKey[] nodeKeys() {
    return new Box.PublicKey[0];
  }

  @Override
  public byte[] decrypt(final EncryptedPayload ciphertextAndMetadata, final Box.PublicKey publicKey) {
    final byte[] cipherText = ciphertextAndMetadata.cipherText();
    final byte[] plainText = new byte[cipherText.length];
    for (int i = 0; i < cipherText.length; i++) {
      final byte b = cipherText[i];
      plainText[i] = (byte) (b - 10);
    }
    return plainText;
  }

  @Override
  public Box.PublicKey readKey(final String b64) {
    return Box.PublicKey.fromBytes(Base64.getDecoder().decode(b64.getBytes(StandardCharsets.UTF_8)));
  }

  @Override
  public EncryptedPayload encrypt(
      final byte[] plaintext,
      final Box.PublicKey senderKey,
      final Box.PublicKey[] recipients,
      final byte[] seed) {
    final byte[] cipherText = new byte[plaintext.length];
    for (int i = 0; i < plaintext.length; i++) {
      final byte b = plaintext[i];
      cipherText[i] = (byte) (b + 10);
    }
    return new EncryptedPayload(
        senderKey,
        new byte[0],
        new EncryptedKey[0],
        cipherText,
        generatePrivacyGroupId(recipients, seed, PrivacyGroupPayload.Type.PANTHEON));
  }

  @Override
  public byte[] generatePrivacyGroupId(
      final Box.PublicKey[] recipientsAndSender,
      final byte[] seed,
      final PrivacyGroupPayload.Type type) {
    return new byte[0];
  }
}
