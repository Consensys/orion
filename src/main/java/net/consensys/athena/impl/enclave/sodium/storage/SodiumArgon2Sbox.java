package net.consensys.athena.impl.enclave.sodium.storage;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.api.enclave.EnclaveException;
import net.consensys.athena.impl.http.data.Base64;

import com.muquit.libsodiumjna.SodiumLibrary;
import com.muquit.libsodiumjna.exceptions.SodiumLibraryException;
import com.sun.jna.NativeLong;

public class SodiumArgon2Sbox {

  public SodiumArgon2Sbox(Config config) {
    SodiumLibrary.setLibraryPath(config.libSodiumPath());
  }

  public byte[] decrypt(StoredPrivateKey storedPrivateKey, String password) {
    ArgonOptions argonOptions = storedPrivateKey.getData().getAopts();
    String asalt = storedPrivateKey.getData().getAsalt();
    int algorithm = lookupAlgorithm(argonOptions);
    try {
      byte[] pwhash =
          SodiumLibrary.cryptoPwhash(
              password.getBytes(),
              decode(asalt),
              argonOptions.getOpsLimit(),
              new NativeLong(argonOptions.getMemLimit()),
              algorithm);
      return SodiumLibrary.cryptoSecretBoxOpenEasy(
          decode(storedPrivateKey.getData().getSbox()),
          decode(storedPrivateKey.getData().getSnonce()),
          pwhash);
    } catch (SodiumLibraryException e) {
      throw new EnclaveException(e);
    }
  }

  private byte[] decode(String asalt) {
    return Base64.decode(asalt);
  }

  private int lookupAlgorithm(ArgonOptions argonOptions) {
    switch (argonOptions.getVariant()) {
      case "i":
        return SodiumLibrary.cryptoPwhashAlgArgon2i13();
      case "id":
        return SodiumLibrary.cryptoPwhashAlgArgon2id13();
      default:
        throw new EnclaveException("Unsupported variant: " + argonOptions.getVariant());
    }
  }

  public byte[] generateAsalt() {
    return SodiumLibrary.randomBytes(SodiumLibrary.cryptoPwhashSaltBytes());
  }

  public byte[] generateSnonce() {
    return SodiumLibrary.randomBytes(SodiumLibrary.cryptoSecretBoxNonceBytes().intValue());
  }

  public byte[] encrypt(
      byte[] privateKey, String password, byte[] asalt, byte[] snonce, ArgonOptions argonOptions) {
    try {
      byte[] pwhash =
          SodiumLibrary.cryptoPwhash(
              password.getBytes(),
              asalt,
              argonOptions.getOpsLimit(),
              new NativeLong(argonOptions.getMemLimit()),
              lookupAlgorithm(argonOptions));
      return SodiumLibrary.cryptoSecretBoxEasy(privateKey, snonce, pwhash);

    } catch (SodiumLibraryException e) {
      throw new EnclaveException(e);
    }
  }
}
