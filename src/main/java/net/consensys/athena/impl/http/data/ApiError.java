package net.consensys.athena.impl.http.data;

import java.util.Objects;

public final class ApiError {
  public final String error;

  public ApiError(String error) {
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
    ApiError apiError = (ApiError) o;
    return Objects.equals(error, apiError.error);
  }

  @Override
  public int hashCode() {

    return Objects.hash(error);
  }
}
