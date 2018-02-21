package net.consensys.orion.impl.http.handler.send;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SendResponse {
  public final String key; // b64 digest key result from encrypted payload storage operation

  @JsonCreator
  public SendResponse(@JsonProperty("key") String key) {
    this.key = key;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SendResponse)) {
      return false;
    }
    SendResponse that = (SendResponse) o;
    return Objects.equals(key, that.key);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key);
  }
}
