package net.consensys.athena.impl.http.handler.receive;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReceiveResponse implements Serializable {
  public String payload;

  @JsonCreator
  public ReceiveResponse(@JsonProperty("payload") String payload) {
    this.payload = payload;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ReceiveResponse)) {
      return false;
    }
    ReceiveResponse that = (ReceiveResponse) o;
    return Objects.equals(payload, that.payload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(payload);
  }
}
