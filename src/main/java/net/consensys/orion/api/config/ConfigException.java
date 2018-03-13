package net.consensys.orion.api.config;

import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.api.exception.OrionException;

public class ConfigException extends OrionException {

  public ConfigException(OrionErrorCode code, String message) {
    super(code, message);
  }
}
