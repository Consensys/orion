package net.consensys.athena.impl.http.handler.send;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SendRequest implements Serializable {
  public String payload; // b64 encoded
  public String from; // b64 encoded
  public String[] to; // b64 encoded

  @JsonCreator
  public SendRequest(
      @JsonProperty("payload") String payload,
      @JsonProperty("from") String from,
      @JsonProperty("to") String[] to) {
    this.payload = payload;
    this.from = from;
    this.to = to;
  }

  @JsonIgnore
  public boolean isValid() {
    if (Stream.of(payload, from, to).anyMatch(Objects::isNull)) {
      return false;
    }
    for (int i = 0; i < to.length; i++) {
      if (to[i].length() <= 0) {
        return false;
      }
    }
    return payload.length() > 0 && from.length() > 0 && to.length > 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SendRequest)) {
      return false;
    }
    SendRequest that = (SendRequest) o;
    return Objects.equals(payload, that.payload)
        && Objects.equals(from, that.from)
        && Arrays.equals(to, that.to);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(payload, from);
    result = 31 * result + Arrays.hashCode(to);
    return result;
  }
}
