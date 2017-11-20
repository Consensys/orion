package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.impl.http.server.Controller;

public class UpcheckController extends StringResponseController implements Controller {

  @Override
  protected String stringResponse() {
    return "I'm up!\n";
  }

  public static final Controller INSTANCE = new UpcheckController();
}
