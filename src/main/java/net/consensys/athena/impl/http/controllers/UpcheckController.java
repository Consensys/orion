package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.impl.http.server.Controller;

/**
 * Simple upcheck/hello check to see if the server is up and running. Returns a 200 response with
 * the body "I'm up!"
 */
public class UpcheckController extends StringResponseController implements Controller {

  @Override
  protected String stringResponse() {
    return "I'm up!\n";
  }

  public static final Controller INSTANCE = new UpcheckController();
}
