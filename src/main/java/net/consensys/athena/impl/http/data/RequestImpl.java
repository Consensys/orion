package net.consensys.athena.impl.http.data;

import java.util.Optional;

public class RequestImpl implements Request {

  private Optional<?> payload;

  public RequestImpl(Optional<?> payload) {
    this.payload = payload;
  }

  @Override
  public <U> U getPayload() {
    // TODO find a nicer way to return a generic optional with clean get API
    return (U) payload.get();
  }
}
