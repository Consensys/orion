package net.consensys.athena.impl.enclave;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.enclave.HashAlgorithm;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageKey;
import net.consensys.athena.impl.enclave.bouncycastle.Hasher;
import net.consensys.athena.impl.storage.SimpleStorage;

import net.consensys.athena.impl.storage.memory.MemoryStorage;
import net.consensys.athena.impl.storage.memory.SimpleMemoryStorage;

import com.codahale.xsalsa20poly1305.SimpleBox;
import okio.ByteString;

import java.security.*;
import java.util.Optional;

import java.util.*;

public class BouncyCastleEnclave implements Enclave {
  static {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  private final Hasher hasher;
  private final Storage storage;
  private final byte[] password;

  public BouncyCastleEnclave() {
    this.hasher = new Hasher();
    this.password = new byte[16];
    this.storage = new SimpleMemoryStorage();
  }

  //Encrypted storage
  public BouncyCastleEnclave(Storage keyStorage, byte[] password) {
    this.hasher = new Hasher();
    this.storage = keyStorage;
    this.password = password;
  }

  @Override
  public byte[] digest(HashAlgorithm algorithm, byte[] input) {
    return hasher.digest(algorithm, input);
  }

  @Override
  public EncryptedPayload encrypt(byte[] plaintext, PublicKey senderKey, PublicKey[] recipients) {

    byte[] senderPublicKey = senderKey.getEncoded();
    byte[] recipientsPublicKey = recipients[0].getEncoded();

    EncryptedPayload payload = encrypt(plaintext, senderPublicKey, recipientsPublicKey);
    return payload;
  }

  public List<EncryptedPayload> encrypt(byte[] plaintext, byte[] senderKey, byte[][] recipients) {

    List<EncryptedPayload> payloads = new ArrayList<>(recipients.length);

    for (int i = 0; i < recipients.length; i++) {
      EncryptedPayload payload = encrypt(plaintext, senderKey, recipients[i]);
      payloads.add(payload);
    }

    return payload;
  }

  public EncryptedPayload encrypt(byte[] plaintext, byte[] senderKey, byte[] recipient) {

    byte[] privateKey = getPrivateKey(senderKey);
    ByteString pk = ByteString.of(privateKey);

    ByteString Pk = ByteString.of(recipient);

    SimpleBox box = new SimpleBox(Pk, pk);
    final ByteString ciphertext = box.seal(ByteString.of(plaintext));

    EncryptedPayload payload = new net.consensys.athena.impl.enclave.EncryptedPayload(senderKey, ciphertext.toByteArray());
    return payload;
  }

  @Override
  public byte[] decrypt(EncryptedPayload ciphertextAndMetadata, PublicKey identity) {

    byte[] ciphertextAndMetadataAsBytes = ciphertextAndMetadata.getCipherText();
    byte[] PK = identity.getEncoded();
    return decrypt(ciphertextAndMetadataAsBytes, PK);
  }

  public byte[] decrypt(byte[] ciphertextAndMetadata, byte[] identity) {

    byte[] privateKey = getNewPrivateKey(identity);

    SimpleBox box = new SimpleBox(ByteString.of(identity), ByteString.of(privateKey));
    ByteString cipher = ByteString.of(ciphertextAndMetadata);
    Optional<ByteString> plainText = box.open(cipher);

    return plainText.get().toByteArray();
  }

  //Helper
  public byte[] generateKeyPair () {
    ByteString privateKey = SimpleBox.generatePrivateKey();
    ByteString publicKey = SimpleBox.generatePublicKey(privateKey);

    return publicKey.toByteArray();
  }

  private byte[] getPrivateKey(byte[] id) {
    StorageKey key = new SimpleStorage(id);
    StorageData data = this.storage.retrieve(key);

    return data.getRaw();
  }
}
