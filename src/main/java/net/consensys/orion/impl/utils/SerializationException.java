package net.consensys.orion.impl.utils;

import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.api.exception.OrionException;

public class SerializationException extends OrionException {

  public SerializationException(OrionErrorCode code, Throwable cause) {
    super(code, cause);
  }
}
