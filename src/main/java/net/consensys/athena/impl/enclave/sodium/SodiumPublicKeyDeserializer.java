package net.consensys.athena.impl.enclave.sodium;

import net.consensys.athena.impl.http.data.Base64;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

public class SodiumPublicKeyDeserializer extends KeyDeserializer {

  @Override
  public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
    return new SodiumPublicKey(Base64.decode(key));
  }
}
