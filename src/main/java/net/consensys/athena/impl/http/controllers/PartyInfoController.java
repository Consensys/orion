package net.consensys.athena.impl.http.controllers;

import static net.consensys.athena.impl.http.data.Result.notImplemented;

import net.consensys.athena.impl.http.data.Request;
import net.consensys.athena.impl.http.data.Result;
import net.consensys.athena.impl.http.server.Controller;

/**
 * Used as a part of the network discovery process. Look up the binary list of constellation nodes
 * that a node has knowledge of.
 */
public class PartyInfoController implements Controller {

  @Override
  public Result handle(Request request) {
    return notImplemented();
  }
}
