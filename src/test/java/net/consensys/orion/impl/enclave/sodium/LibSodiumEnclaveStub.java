package net.consensys.orion.impl.enclave.sodium;

import net.consensys.orion.api.enclave.CombinedKey;
import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.enclave.PublicKey;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class LibSodiumEnclaveStub implements Enclave {

  @Override
  public PublicKey[] alwaysSendTo() {
    return new PublicKey[0];
  }

  @Override
  public PublicKey[] nodeKeys() {
    return new PublicKey[0];
  }

  @Override
  public byte[] decrypt(EncryptedPayload ciphertextAndMetadata, PublicKey publicKey) {
    byte[] cipherText = ciphertextAndMetadata.cipherText();
    byte[] plainText = new byte[cipherText.length];
    for (int i = 0; i < cipherText.length; i++) {
      byte b = cipherText[i];
      plainText[i] = (byte) (b - 10);
    }
    return plainText;
  }

  @Override
  public PublicKey readKey(String b64) {
    return new PublicKey(Base64.getDecoder().decode(b64.getBytes(StandardCharsets.UTF_8)));
  }

  @Override
  public EncryptedPayload encrypt(byte[] plaintext, PublicKey senderKey, PublicKey[] recipients) {
    byte[] cipherText = new byte[plaintext.length];
    for (int i = 0; i < plaintext.length; i++) {
      byte b = plaintext[i];
      cipherText[i] = (byte) (b + 10);
    }
    CombinedKey[] combinedKeys = new CombinedKey[0];
    byte[] combinedKeyNonce = {};
    byte[] nonce = {};
    return new EncryptedPayload(senderKey, nonce, combinedKeyNonce, combinedKeys, cipherText);
  }
}
