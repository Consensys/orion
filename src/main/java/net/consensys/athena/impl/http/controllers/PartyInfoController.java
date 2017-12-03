package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.impl.http.responders.PartyInfoResponder;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Responder;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

/**
 * Used as a part of the network discovery process. Look up the binary list of constellation nodes
 * that a node has knowledge of.
 */
public class PartyInfoController implements Controller {

  @Override
  public Responder handle(HttpRequest request, FullHttpResponse response) {
    return new PartyInfoResponder(response);
  }
}
