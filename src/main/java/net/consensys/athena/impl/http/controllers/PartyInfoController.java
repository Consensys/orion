package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Request;
import net.consensys.athena.impl.http.data.Result;
import net.consensys.athena.impl.http.server.Controller;

/**
 * Used as a part of the network discovery process. Look up the binary list of constellation nodes
 * that a node has knowledge of.
 */
public class PartyInfoController implements Controller {

  private static NetworkNodes networkNodes;

  public PartyInfoController(NetworkNodes info) {
    networkNodes = info;
  }

  @Override
  public Result handle(Request request) {
    return Result.ok(ContentType.CBOR, networkNodes);
  }
}
