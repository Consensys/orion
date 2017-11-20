package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.impl.http.server.Controller;

public class UpcheckController extends HandleStringController implements Controller {

  @Override
  protected String handleString() {
    return "I'm up!\n";
  }
}
