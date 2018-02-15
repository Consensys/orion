package net.consensys.athena.impl.enclave.sodium;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.enclave.HashAlgorithm;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
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
  public byte[] digest(HashAlgorithm algorithm, byte[] input) {
    byte[] hash = new byte[256];
    for (int i = 0; i < input.length && i < 256; i++) {
      hash[i] = input[i];
    }
    return hash;
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
    return new SodiumPublicKey(Base64.getDecoder().decode(b64.getBytes(StandardCharsets.UTF_8)));
  }

  @Override
  public EncryptedPayload encrypt(byte[] plaintext, PublicKey senderKey, PublicKey[] recipients) {
    byte[] ciphterText = new byte[plaintext.length];
    for (int i = 0; i < plaintext.length; i++) {
      byte b = plaintext[i];
      ciphterText[i] = (byte) (b + 10);
    }
    SodiumCombinedKey[] combinedKeys = new SodiumCombinedKey[0];
    byte[] combinedKeyNonce = {};
    byte[] nonce = {};
    return new SodiumEncryptedPayload(
        (SodiumPublicKey) senderKey, nonce, combinedKeyNonce, combinedKeys, ciphterText);
  }
}
