package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.impl.http.server.Controller;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

/**
 * ask to resend a single transaction or all transactions. Useful in situations where a
 * constellation node has lost it's database and wants to recover lost transactions.
 */
public class ResendController implements Controller {

  public static final Controller INSTANCE = new ResendController();

  @Override
  public FullHttpResponse handle(HttpRequest request, FullHttpResponse response) {
    return response;
  }
}
