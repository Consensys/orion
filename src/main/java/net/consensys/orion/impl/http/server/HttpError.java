package net.consensys.orion.impl.http.server;

import net.consensys.orion.impl.exception.OrionErrorCode;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class HttpError {
  private static final Logger log = LogManager.getLogger();

  private OrionErrorCode error;

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
  public int error() {
    return error.code();
  }

  @JsonProperty("error")
  public void error(final int code) {
    final Optional<OrionErrorCode> potential = OrionErrorCode.get(code);

    if (potential.isPresent()) {
      this.error = potential.get();
    } else {
      log.warn(
          String.format(
              "Unmapped error code, decimal: %s, hex: %s", code, Integer.toBinaryString(code)));
      this.error = OrionErrorCode.UNMAPPED;
    }
  }
}
