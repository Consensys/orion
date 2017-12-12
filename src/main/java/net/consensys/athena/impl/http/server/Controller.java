package net.consensys.athena.impl.http.server;

public interface Controller {
  Result handle(Request request) throws Exception;

  // returns the expected request class, used for deserialization purposes
  default Class<?> expectedRequest() {
    return EmptyPayload.class;
  }
}
