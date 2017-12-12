package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.impl.http.server.ContentType;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Request;
import net.consensys.athena.impl.http.server.Result;

/**
 * Simple upcheck/hello check to see if the server is up and running. Returns a 200 response with
 * the body "I'm up!"
 */
public class UpcheckController implements Controller {

  @Override
  public Result handle(Request request) {
    return Result.ok(ContentType.RAW, "I'm up!");
  }
}
