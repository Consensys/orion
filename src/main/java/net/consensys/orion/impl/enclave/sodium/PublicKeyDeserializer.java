package net.consensys.orion.impl.enclave.sodium;

import net.consensys.orion.api.enclave.PublicKey;
import net.consensys.orion.impl.utils.Base64;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

public class PublicKeyDeserializer extends KeyDeserializer {

  @Override
  public Object deserializeKey(String key, DeserializationContext ctxt) {
    return new PublicKey(Base64.decode(key));
  }
}
