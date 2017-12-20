package net.consensys.athena.impl.enclave.sodium;

import net.consensys.athena.api.enclave.EnclaveException;
import net.consensys.athena.impl.http.data.Base64;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.PublicKey;

public class SodiumPublicKeyReader {
  public PublicKey readPublicKey(File publicKeyFile) {
    try (BufferedReader br = new BufferedReader(new FileReader(publicKeyFile))) {
      String base64Encoded = br.readLine();
      byte[] decoded = Base64.decode(base64Encoded);
      return new SodiumPublicKey(decoded);
    } catch (IOException e) {
      throw new EnclaveException(e);
    }
  }
}
