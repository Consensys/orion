package net.consensys.athena.impl.http.handler.receive;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReceiveResponse implements Serializable {
  public String payload;

  @JsonCreator
  ReceiveResponse(@JsonProperty("payload") String payload) {
    this.payload = payload;
  }
}
