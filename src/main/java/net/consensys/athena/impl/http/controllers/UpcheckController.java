package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Request;
import net.consensys.athena.impl.http.data.Result;
import net.consensys.athena.impl.http.server.Controller;

/**
 * Simple upcheck/hello check to see if the server is up and running. Returns a 200 response with
 * the body "I'm up!"
 */
public class UpcheckController implements Controller {

  @Override
  public Result handle(Request request) {
    return Result.ok(ContentType.TEXT, "I'm up!");
  }
}
