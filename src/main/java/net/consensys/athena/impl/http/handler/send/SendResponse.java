package net.consensys.athena.impl.http.handler.send;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SendResponse {
  public String key; // b64 digest key result from encrypted payload storage operation

  @JsonCreator
  public SendResponse(@JsonProperty("key") String key) {
    this.key = key;
  }
}
