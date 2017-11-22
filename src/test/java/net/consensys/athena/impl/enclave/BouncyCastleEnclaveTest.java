package net.consensys.athena.impl.enclave;

import static net.consensys.athena.api.enclave.HashAlgorithm.SHA_512_256;
import static org.junit.Assert.*;

import net.consensys.athena.api.enclave.HashAlgorithm;

import java.util.Arrays;
import java.util.Collection;
import javax.xml.bind.DatatypeConverter;

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
}
