package net.consensys.orion.impl.enclave.sodium.storage;

import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.impl.utils.Base64;

import com.muquit.libsodiumjna.SodiumLibrary;
import com.muquit.libsodiumjna.exceptions.SodiumLibraryException;
import com.sun.jna.NativeLong;

public class SodiumArgon2Sbox {

  public SodiumArgon2Sbox(Config config) {
    SodiumLibrary.setLibraryPath(config.libSodiumPath());
  }

  public byte[] decrypt(StoredPrivateKey storedPrivateKey, String password) {
    final ArgonOptions argonOptions = storedPrivateKey.data().aopts().get();
    final String asalt = storedPrivateKey.data().asalt().get();
    final int algorithm = lookupAlgorithm(argonOptions);
    try {
      final byte[] pwhash =
          SodiumLibrary.cryptoPwhash(
              password.getBytes(),
              decode(asalt),
              argonOptions.opsLimit().get(),
              new NativeLong(argonOptions.memLimit().get()),
              algorithm);
      return SodiumLibrary.cryptoSecretBoxOpenEasy(
          decode(storedPrivateKey.data().sbox().get()),
          decode(storedPrivateKey.data().snonce().get()),
          pwhash);
    } catch (final SodiumLibraryException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_STORAGE_DECRYPT, e);
    }
  }

  private byte[] decode(String asalt) {
    return Base64.decode(asalt);
  }

  private int lookupAlgorithm(ArgonOptions argonOptions) {
    switch (argonOptions.variant()) {
      case "i":
        return SodiumLibrary.cryptoPwhashAlgArgon2i13();
      case "id":
        return SodiumLibrary.cryptoPwhashAlgArgon2id13();
      default:
        throw new EnclaveException(
            OrionErrorCode.ENCLAVE_UNSUPPORTED_STORAGE_ALGORTHIM,
            "Unsupported variant: " + argonOptions.variant());
    }
  }

  public byte[] generateAsalt() {
    return SodiumLibrary.randomBytes(SodiumLibrary.cryptoPwhashSaltBytes());
  }

  public byte[] generateSnonce() {
    return SodiumLibrary.randomBytes(SodiumLibrary.cryptoSecretBoxNonceBytes().intValue());
  }

  public byte[] encrypt(
      final byte[] privateKey,
      String password,
      byte[] asalt,
      byte[] snonce,
      ArgonOptions argonOptions) {
    try {
      final byte[] pwhash =
          SodiumLibrary.cryptoPwhash(
              password.getBytes(),
              asalt,
              argonOptions.opsLimit().get(),
              new NativeLong(argonOptions.memLimit().get()),
              lookupAlgorithm(argonOptions));
      return SodiumLibrary.cryptoSecretBoxEasy(privateKey, snonce, pwhash);

    } catch (final SodiumLibraryException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_STORAGE_ENCRYPT, e);
    }
  }
}
