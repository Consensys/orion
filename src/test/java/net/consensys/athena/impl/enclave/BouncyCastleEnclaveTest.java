package net.consensys.athena.impl.enclave;

import static net.consensys.athena.api.enclave.HashAlgorithm.SHA_512_256;
import static org.junit.Assert.*;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.HashAlgorithm;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyFactory;


import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Collection;
import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.jce.spec.ECParameterSpec;
//import org.bouncycastle.jce.spec.ECN

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;


@RunWith(Parameterized.class)
public class BouncyCastleEnclaveTest {

  private final HashAlgorithm algorithm;
  private final String plaintext;
  private final String expectedHash;
  // test vectors taken from
  // https://csrc.nist.gov/CSRC/media/Projects/Cryptographic-Standards-and-Guidelines/documents/examples/SHA512_256.pdf
  @Parameterized.Parameters
  public static Collection testVectors() {
    return Arrays.asList(
        new Object[][] {
          {SHA_512_256, "abc", "53048E2681941EF99B2E29B76B4C7DABE4C2D0C634FC6D46E0E2F13107E7AF23"},
          {
            SHA_512_256,
            "abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmnhijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu",
            "3928E184FB8690F840DA3988121D31BE65CB9D3EF83EE6146FEAC861E19B563A"
          }
        });
  }

  public BouncyCastleEnclaveTest(HashAlgorithm algorithm, String plaintext, String expectedHash) {
    this.algorithm = algorithm;
    this.plaintext = plaintext;
    this.expectedHash = expectedHash;
  }

  @Test
  public void testHashingUsingTestVector() {
    // a simple hello world test of hashing a string
    byte[] digest = new BouncyCastleEnclave().digest(algorithm, plaintext.getBytes());
    String hex = DatatypeConverter.printHexBinary(digest);
    assertEquals(expectedHash, hex);
  }

  @Test
  public void testShouldEncryptData2() throws Exception {
    try {
      byte[] enclavePassword = new byte[16];
      BouncyCastleEnclave enclave = new BouncyCastleEnclave(new MockKeyStorage(), enclavePassword);

      byte[] m = new byte[32];


      //Hex.decode("0xbe,0x07,0x5f,0xc5,0x3c,0x81,0xf2,0xd5,0xcf,0x14,0x13,0x16,0xeb,0xeb,0x0c,0x7b,0x52,0x28,0xc5,0x2a,0x4c,0x62,0xcb,0xd4,0x4b,0x66,0x84,0x9b,0x64,0x24,0x4f,0xfc");// +
//              "     ,0xe5,0xec,0xba,0xaf,0x33,0xbd,0x75,0x1a\n" +
//              "     ,0x1a,0xc7,0x28,0xd4,0x5e,0x6c,0x61,0x29\n" +
//              "     ,0x6c,0xdc,0x3c,0x01,0x23,0x35,0x61,0xf4\n" +
//              "     ,0x1d,0xb6,0x6c,0xce,0x31,0x4a,0xdb,0x31\n" +
//              "     ,0x0e,0x3b,0xe8,0x25,0x0c,0x46,0xf0,0x6d\n" +
//              "     ,0xce,0xea,0x3a,0x7f,0xa1,0x34,0x80,0x57\n" +
//              "     ,0xe2,0xf6,0x55,0x6a,0xd6,0xb1,0x31,0x8a\n" +
//              "     ,0x02,0x4a,0x83,0x8f,0x21,0xaf,0x1f,0xde\n" +
//              "     ,0x04,0x89,0x77,0xeb,0x48,0xf5,0x9f,0xfd\n" +
//              "     ,0x49,0x24,0xca,0x1c,0x60,0x90,0x2e,0x52\n" +
//              "     ,0xf0,0xa0,0x89,0xbc,0x76,0x89,0x70,0x40\n" +
//              "     ,0xe0,0x82,0xf9,0x37,0x76,0x38,0x48,0x64\n" +
//              "     ,0x5e,0x07,0x05");


      //byte[] bobpk = Hex.decode("0xde,0x9e,0xdb,0x7d,0x7b,0x7d,0xc1,0xb4,0xd3,0x5b,0x61,0xc2,0xec,0xe4,0x35,0x37,0x3f,0x83,0x43,0xc8,0x5b,0x78,0x67,0x4d,0xad,0xfc,0x7e,0x14,0x6f,0x88,0x2b,0x4f");
      //byte[] bobpk = Byte.decode("0xde,0x9e,0xdb,0x7d,0x7b,0x7d,0xc1,0xb4,0xd3,0x5b,0x61,0xc2,0xec,0xe4,0x35,0x37,0x3f,0x83,0x43,0xc8,0x5b,0x78,0x67,0x4d,0xad,0xfc,0x7e,0x14,0x6f,0x88,0x2b,0x4f");

      byte[] bobpk = new byte[32];
      assertEquals(32, bobpk.length);

      enclave.encrypt(m, bobpk, bobpk);
    }
    catch (Exception ex) {
      throw ex;
    }
  }

  @Test
  public void testShouldEncryptData() {
    Enclave enclave = new BouncyCastleEnclave(new MockKeyStorage(), new byte[16]);


    byte[] m = Hex.decode("0xbe,0x07,0x5f,0xc5,0x3c,0x81,0xf2,0xd5\n" +
            "     ,0xcf,0x14,0x13,0x16,0xeb,0xeb,0x0c,0x7b\n" +
            "     ,0x52,0x28,0xc5,0x2a,0x4c,0x62,0xcb,0xd4\n" +
            "     ,0x4b,0x66,0x84,0x9b,0x64,0x24,0x4f,0xfc\n" +
            "     ,0xe5,0xec,0xba,0xaf,0x33,0xbd,0x75,0x1a\n" +
            "     ,0x1a,0xc7,0x28,0xd4,0x5e,0x6c,0x61,0x29\n" +
            "     ,0x6c,0xdc,0x3c,0x01,0x23,0x35,0x61,0xf4\n" +
            "     ,0x1d,0xb6,0x6c,0xce,0x31,0x4a,0xdb,0x31\n" +
            "     ,0x0e,0x3b,0xe8,0x25,0x0c,0x46,0xf0,0x6d\n" +
            "     ,0xce,0xea,0x3a,0x7f,0xa1,0x34,0x80,0x57\n" +
            "     ,0xe2,0xf6,0x55,0x6a,0xd6,0xb1,0x31,0x8a\n" +
            "     ,0x02,0x4a,0x83,0x8f,0x21,0xaf,0x1f,0xde\n" +
            "     ,0x04,0x89,0x77,0xeb,0x48,0xf5,0x9f,0xfd\n" +
            "     ,0x49,0x24,0xca,0x1c,0x60,0x90,0x2e,0x52\n" +
            "     ,0xf0,0xa0,0x89,0xbc,0x76,0x89,0x70,0x40\n" +
            "     ,0xe0,0x82,0xf9,0x37,0x76,0x38,0x48,0x64\n" +
            "     ,0x5e,0x07,0x05");

    X9ECParameters ecP = CustomNamedCurves.getByName("curve25519");
    org.bouncycastle.jce.spec.ECParameterSpec spec = new ECParameterSpec(ecP.getCurve(), ecP.getG(), ecP.getN(), ecP.getH(), ecP.getSeed());

    //java.security.spec.ECPrivateKeySpec spec2 = new ECParameterSpec(ecP.getCurve(), ecP.getG(), ecP.getN(), ecP.getH(), ecP.getSeed());
    //ECParameterSpec spec = org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("curve25519");

    java.math.BigInteger s = new java.math.BigInteger("876300101507107567501066130761671078357010671067781776716671676178726717");
    //ECPrivateKey key = new java.security.spec.ECPrivateKeySpec(s, spec);
    org.bouncycastle.jce.spec.ECPrivateKeySpec key = new org.bouncycastle.jce.spec.ECPrivateKeySpec(s, spec);
    //PublicKey Pk = k

    //ECPublicKeySpec pubKey = new ECPublicKeySpec(ECPointUtil.decodePoint(ecP.getCurve(), Hex.decode("025b6dc53bc61a2548ffb0f671472de6c9521a9d2d2534e65abfcbd5fe0c70")), spec);

    //ECParameterSpec spec = new ECNamedCurveSpec(org.bouncycastle.jce.spec.ECNamedCurveParameterSpec.getParameterSpec("curve25519"));
    //ECPrivateKeySpec priKey = new ECPrivateKeySpec(new java.math.BigInteger("876300101507107567501066130761671078357010671067781776716671676178726717"), spec);
    //PrivateKey privateKey = kf.generatePrivate(keySpec);

    //enclave.encrypt(m, key, null);
  }


//  @Test
//  public void testShouldEncryptData2() throws Exception {
//    try {
//      Enclave enclave = new BouncyCastleEnclave(new MockKeyStorage(), new byte[16]);
//
//      byte[] plainText = new byte[32];
//
//      ECParameterSpec parameterSpec = org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("Curve25519");
//      java.security.KeyPairGenerator g = java.security.KeyPairGenerator.getInstance("ECDH", "BC");
////
//      g.initialize(parameterSpec);
//      KeyPair keyPair = g.generateKeyPair();
//
//      PublicKey pk = keyPair.getPublic();
//
//      enclave.encrypt(plainText, pk, null);
//    }
//    catch (Exception ex) {
//      throw ex;
//    }
//  }

  @Test
  public void testShould_Generate_New_PrivateKey() {
    try {
      BouncyCastleEnclave enclave = new BouncyCastleEnclave();
      byte[] actual = enclave.generateKeyPair();

      assertNotNull(actual);

//      PrivateKey k = actual.getPrivate();
//      PublicKey K = actual.getPublic();
//
//      assertNotNull(k);
//      assertNotNull(K);
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}