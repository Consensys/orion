package net.consensys.athena.impl.http.handler.receive;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReceiveRequest implements Serializable {
  public String key;
  public String publicKey; // b64 encoded

  @JsonCreator
  public ReceiveRequest(
      @JsonProperty("key") String key, @JsonProperty("publicKey") String publicKey) {
    this.key = key;
    this.publicKey = publicKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ReceiveRequest)) {
      return false;
    }
    ReceiveRequest that = (ReceiveRequest) o;
    return Objects.equals(key, that.key) && Objects.equals(publicKey, that.publicKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, publicKey);
  }
}
