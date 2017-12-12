package net.consensys.athena.impl.http.server;

import static java.util.Optional.empty;

import java.util.Optional;

public interface Controller {
  Result handle(Request request) throws Exception;

  // returns the expected request class, used for deserialization purposes
  default Optional<Class<?>> expectedRequest() {
    return empty();
  }
}
