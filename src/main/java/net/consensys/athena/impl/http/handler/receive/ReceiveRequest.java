package net.consensys.athena.impl.http.handler.receive;

import net.consensys.athena.impl.enclave.sodium.SodiumPublicKey;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReceiveRequest implements Serializable {
  public String key;
  public SodiumPublicKey publicKey;

  @JsonCreator
  public ReceiveRequest(
      @JsonProperty("key") String key, @JsonProperty("publicKey") SodiumPublicKey publicKey) {
    this.key = key;
    this.publicKey = publicKey;
  }
}
