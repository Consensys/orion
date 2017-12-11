package net.consensys.athena.api.config;

public class ConfigException extends RuntimeException {

  public ConfigException(String message) {
    super(message);
  }

  public ConfigException(String message, Throwable cause) {
    super(message, cause);
  }

  public ConfigException(Throwable cause) {
    super(cause);
  }

  protected ConfigException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
