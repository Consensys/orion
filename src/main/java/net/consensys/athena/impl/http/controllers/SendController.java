package net.consensys.athena.impl.http.controllers;

public class SendController extends StringResponseController {

  @Override
  protected String stringResponse() {
    return "{\"key\":\"abcd\"}";
  }
}
