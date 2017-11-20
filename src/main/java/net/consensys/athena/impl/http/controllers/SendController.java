package net.consensys.athena.impl.http.controllers;

public class SendController extends HandleStringController {

  @Override
  protected String handleString() {
    return "{\"key\":\"abcd\"}";
  }
}
