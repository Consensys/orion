package net.consensys.athena.impl.enclave.sodium;

import net.consensys.athena.api.enclave.CombinedKey;

public class SodiumCombinedKey implements CombinedKey {

  byte[] key;

  public SodiumCombinedKey(byte[] key) {
    this.key = key;
  }

  @Override
  public byte[] getEncoded() {
    return key;
  }
}
