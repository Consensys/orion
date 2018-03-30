package net.consensys.orion.impl.http.server;

import net.consensys.orion.api.exception.OrionErrorCode;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class HttpError {

  private final OrionErrorCode error;

  public HttpError(OrionErrorCode error) {
    this.error = error;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HttpError httpError = (HttpError) o;
    return Objects.equals(error, httpError.error);
  }

  @Override
  public int hashCode() {
    return Objects.hash(error);
  }

  @Override
  public String toString() {
    return "HttpError{" + "error='" + error.code() + '\'' + '}';
  }

  @JsonProperty("error")
  public String error() {
    return error.code();
  }
}
