package net.consensys.athena.impl.enclave;

import net.consensys.athena.api.enclave.EncryptedPayload;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

public class EncryptedPayloadBuilder {
  private final byte[] data;

  public EncryptedPayloadBuilder(byte[] data) {
    this.data = data;
  }

  public EncryptedPayload build() {
    // for now, let's do Java object serialization...
    // TODO ; use same serizalization as send operation used to create the EncryptedPayload ! @see haskell compat
    return javaObjDeserialize();
  }

  private EncryptedPayload javaObjDeserialize() {
    try {
      ByteArrayInputStream in = new ByteArrayInputStream(data);
      ObjectInputStream is = new ObjectInputStream(in);
      return (SimpleEncryptedPayload) is.readObject();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
