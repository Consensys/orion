package net.consensys.orion.impl.http.handler.send;

import net.consensys.orion.impl.utils.Base64;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SendRequest implements Serializable {
  private final String from; // b64 encoded
  private final String[] to; // b64 encoded
  private final byte[] rawPayload;

  @JsonProperty("from")
  public Optional<String> from() {
    return Optional.ofNullable(from);
  }

  @JsonProperty("to")
  public String[] to() {
    return to;
  }

  @JsonProperty("payload")
  public String payload() {
    if (rawPayload == null) {
      return null;
    }
    return Base64.encode(rawPayload);
  }

  @JsonCreator
  public SendRequest(
      @JsonProperty("payload") String payload,
      @JsonProperty("from") String from,
      @JsonProperty("to") String[] to) {
    this(decodePayload(payload), from, to);
  }

  private static byte[] decodePayload(String payload) {
    if (payload != null) {
      return Base64.decode(payload);
    } else {
      return new byte[0];
    }
  }

  public SendRequest(byte[] rawPayload, String from, String[] to) {
    this.rawPayload = rawPayload;
    this.from = from;
    this.to = to;
  }

  @JsonIgnore
  public boolean isValid() {
    if (Stream.of(rawPayload, to).anyMatch(Objects::isNull)) {
      return false;
    }
    for (int i = 0; i < to.length; i++) {
      if (to[i].length() <= 0) {
        return false;
      }
    }
    return rawPayload.length >= 0 && (from == null || from.length() > 0) && to.length > 0;
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
    return Arrays.equals(rawPayload, that.rawPayload)
        && Objects.equals(from, that.from)
        && Arrays.equals(to, that.to);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(from);
    result = 31 * result + Arrays.hashCode(rawPayload);
    result = 31 * result + Arrays.hashCode(to);
    return result;
  }

  @Override
  public String toString() {
    return "SendRequest{"
        + "payload='"
        + Base64.encode(rawPayload)
        + '\''
        + ", from='"
        + from
        + '\''
        + ", to="
        + Arrays.toString(to)
        + '}';
  }

  public byte[] rawPayload() {
    return rawPayload;
  }
}
