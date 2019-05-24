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
package net.consensys.orion.helpers;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.orion.enclave.Enclave;
import net.consensys.orion.enclave.EncryptedKey;
import net.consensys.orion.enclave.EncryptedPayload;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * A very simple test class that implements the enclave interface and does minimal encryption operations that do not do
 * much at all.
 */
public class StubEnclave implements Enclave {

  private final Box.PublicKey[] alwaysSendTo;
  private final Box.PublicKey[] nodeKeys;

  public StubEnclave() {
    this.alwaysSendTo = new Box.PublicKey[0];
    this.nodeKeys = new Box.PublicKey[0];
  }

  @Override
  public Box.PublicKey[] alwaysSendTo() {
    return alwaysSendTo;
  }

  @Override
  public Box.PublicKey[] nodeKeys() {
    return nodeKeys;
  }

  @Override
  public byte[] decrypt(EncryptedPayload ciphertextAndMetadata, Box.PublicKey publicKey) {
    byte[] cipherText = ciphertextAndMetadata.cipherText();
    byte[] plainText = new byte[cipherText.length];
    for (int i = 0; i < cipherText.length; i++) {
      byte b = cipherText[i];
      plainText[i] = (byte) (b - 10);
    }
    return plainText;
  }

  @Override
  public Box.PublicKey readKey(String b64) {
    return Box.PublicKey.fromBytes(Base64.getDecoder().decode(b64.getBytes(StandardCharsets.UTF_8)));
  }

  @Override
  public EncryptedPayload encrypt(byte[] plaintext, Box.PublicKey senderKey, Box.PublicKey[] recipients) {
    byte[] ciphterText = new byte[plaintext.length];
    for (int i = 0; i < plaintext.length; i++) {
      byte b = plaintext[i];
      ciphterText[i] = (byte) (b + 10);
    }

    byte[] nonce = {};

    EncryptedKey[] encryptedKeys;
    Map<Box.PublicKey, Integer> encryptedKeyOwners = new HashMap<>();
    ArrayList<Box.PublicKey> keys = new ArrayList<>();

    if (recipients != null && recipients.length > 0) {
      encryptedKeys = new EncryptedKey[recipients.length];
      keys = new ArrayList<>(Arrays.stream(recipients).collect(Collectors.toList()));
      for (int i = 0; i < recipients.length; i++) {
        encryptedKeyOwners.put(recipients[i], i);
        encryptedKeys[i] = new EncryptedKey(recipients[i].bytesArray());
      }
    } else {
      encryptedKeys = new EncryptedKey[0];
    }

    if (senderKey != null) {
      keys.add(senderKey);
    }
    return new EncryptedPayload(
        senderKey,
        nonce,
        encryptedKeys,
        ciphterText,
        encryptedKeyOwners,
        generatePrivacyGroupId(keys.toArray(new Box.PublicKey[0])));
  }

  @Override
  public byte[] generatePrivacyGroupId(Box.PublicKey[] recipientsAndSender) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    for (int i = 0; recipientsAndSender != null && i < recipientsAndSender.length; i++) {
      outputStream.write(i);
    }

    return outputStream.toByteArray();
  }
}
