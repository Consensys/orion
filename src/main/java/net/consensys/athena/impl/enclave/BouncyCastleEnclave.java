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

import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;

import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.macs.Poly1305;

import javax.crypto.KeyAgreement;

import org.bouncycastle.crypto.generators.Poly1305KeyGenerator;


import org.bouncycastle.crypto.macs.Poly1305;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.Pack;

import java.security.*;


public class BouncyCastleEnclave implements Enclave {
  static {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  private final Hasher hasher;
  private final Storage storage;
  private final byte[] password;

  private final String CURVE_NAME = "curve25519";
  private final String MAC = "Poly1305";

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

    byte[] privateKey = getPrivateKey(senderKey.getEncoded());

    EncryptedPayload payload = new net.consensys.athena.impl.enclave.EncryptedPayload(1);

    return payload;
  }

  @Override
  public byte[] decrypt(EncryptedPayload ciphertextAndMetadata, PublicKey identity) {

    return new byte[0];
  }

  public KeyPair generateKeyPair () throws Exception {
    try {
      //X9ECParameters c25519 = CustomNamedCurves.getByName("Curve25519");
      //ECDomainParameters ecSpec = new ECDomainParameters(c25519.getCurve(), c25519.getG(), c25519.getN());

      ECParameterSpec parameterSpec = org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec(CURVE_NAME);
      KeyPairGenerator g = KeyPairGenerator.getInstance("ECDH", "BC");

      g.initialize(parameterSpec);

      return g.generateKeyPair();
    }
    catch (NoSuchAlgorithmException ex) {
      throw ex;
    }
  }

  private byte[] getPrivateKey(byte[] id) {
    StorageKey key = new SimpleStorage(id);
    StorageData data = this.storage.retrieve(key);

    return data.getRaw();
  }

  private byte[] encrypt(PrivateKey pk, byte[] data) {
    return new byte[0];
  }

  //Public Key, Secret Key
  public byte[] generateKeyAgreement(PublicKey pk, PrivateKey sk) throws Exception {
    //https://github.com/bcgit/bc-java/blob/8ed589d753a41c6d4da918321657a1b40e0ec5fd/prov/src/test/java/org/bouncycastle/jce/provider/test/DHTest.java

    try {
      KeyAgreement aKeyAgree = KeyAgreement.getInstance("ECDH", "BC");
      aKeyAgree.init(sk);

      //aKeyAgree.doPhase();

      return new byte[0];
    }
    catch (Exception ex) {
      throw ex;
    }
  }

  public byte[] generateMac(byte[] key, long nonce, byte[] message) {
    //this.key = Hex.decode(key);
    // nacl test case keys are not pre-clamped
    Poly1305KeyGenerator.clamp(key);

    //this.nonce = (nonce == null) ? null : Hex.decode(nonce);
    byte[] nonceArray = new byte[24];

    //this.message = Hex.decode(message);
    //this.expectedMac = Hex.decode(expectedMac);

    Mac mac = new Poly1305();

    return new byte[0];
  }

  public byte[] generateMac2(byte[] key, byte[] nonce, byte[] message) {



    //this.key = Hex.decode(key);
    // nacl test case keys are not pre-clamped
    Poly1305KeyGenerator.clamp(key);

    //this.nonce = (nonce == null) ? null : Hex.decode(nonce);

    //this.message = Hex.decode(message);
    //this.expectedMac = Hex.decode(expectedMac);

    Mac mac = new Poly1305();

    return new byte[0];
  }

  public static byte[] create(KeyParameter macKey, byte[] ciphertext) {
    Poly1305 poly = new Poly1305();
    poly.init(macKey);

    poly.update(ciphertext, 0, ciphertext.length);
    if ((ciphertext.length % 16) != 0) {
      int round = 16-(ciphertext.length%16);
      poly.update(new byte[round], 0, round);
    }

    byte[] ciphertextLength = Pack.longToLittleEndian(ciphertext.length);
    poly.update(ciphertextLength, 0, 8);

    byte[] calculatedMAC = new byte[poly.getMacSize()];
    poly.doFinal(calculatedMAC, 0);

    return calculatedMAC;
  }
}
