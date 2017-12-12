package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Result;

import io.netty.handler.codec.http.FullHttpRequest;

/**
 * Used as a part of the network discovery process. Look up the binary list of constellation nodes
 * that a node has knowledge of.
 */
public class PartyInfoController implements Controller {

  @Override
  public Result handle(FullHttpRequest request) {
    return Result.notImplemented();
  }
}
