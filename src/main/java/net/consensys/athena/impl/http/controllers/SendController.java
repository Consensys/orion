package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.impl.http.server.Controller;

/** Send a base64 encoded payload to encrypt. */
public class SendController extends StringResponseController {

  public static final Controller INSTANCE = new SendController();

  @Override
  protected String stringResponse() {
    return "{\"key\":\"abcd\"}";
  }
}
