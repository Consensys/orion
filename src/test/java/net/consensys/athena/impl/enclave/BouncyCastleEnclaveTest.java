package net.consensys.athena.impl.enclave;

import static net.consensys.athena.api.enclave.HashAlgorithm.SHA_512_256;
import static org.junit.Assert.*;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.HashAlgorithm;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collection;
import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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
  public void testShouldEncryptData() {
    Enclave enclave = new BouncyCastleEnclave(new MockKeyStorage(), new byte[16]);

    byte[] plainText = new byte[32];

    //enclave.encrypt(plainText);
  }

  @Test
  public void testShouldGetNewPrivateKey() {
    try {
      BouncyCastleEnclave enclave = new BouncyCastleEnclave();
      KeyPair actual = enclave.generateKeyPair();

      assertNotNull(actual);

      PrivateKey k = actual.getPrivate();
      PublicKey K = actual.getPublic();

      assertNotNull(k);
      assertNotNull(K);
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  @Test
  public void testShould_Generate_New_MAC() {
    //Test vectors from https://tools.ietf.org/html/rfc7539#section-2.6.2

    Enclave enclave = new BouncyCastleEnclave(new MockKeyStorage(), new byte[16]);

    byte[] message = Hex.decode("80 81 82 83 84 85 86 87 88 89 8a 8b 8c 8d 8e 8f 90 91 92 93 94 95 96 97 98 99 9a 9b 9c 9d 9e 9f");
    byte[] nonce = Hex.decode("00 00 00 00 00 01 02 03 04 05 06 07");

    assertTrue(nonce.length == 12);
    assertTrue(message.length == 32);

    //enclave.g();

  }
}