package net.consensys.athena.api.cmd;

import net.consensys.athena.impl.http.controllers.DeleteController;
import net.consensys.athena.impl.http.controllers.ReceiveController;
import net.consensys.athena.impl.http.controllers.ReceiveRawController;
import net.consensys.athena.impl.http.controllers.SendController;
import net.consensys.athena.impl.http.controllers.SendRawController;
import net.consensys.athena.impl.http.controllers.UpcheckController;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Router;

import java.net.URI;
import java.net.URISyntaxException;

import io.netty.handler.codec.http.HttpRequest;

public class AthenaRouter implements Router {

  @Override
  public Controller lookup(HttpRequest request) {
    try {
      URI uri = new URI(request.uri());
      //TODO make the following use an ordered map (LinkedHashMap).
      if (uri.getPath().startsWith("/upcheck")) {
        return new UpcheckController();
      }
      if (uri.getPath().startsWith("/sendraw")) {
        return new SendRawController();
      }
      if (uri.getPath().startsWith("/receiveraw")) {
        return new ReceiveRawController();
      }
      if (uri.getPath().startsWith("/send")) {
        return new SendController();
      }
      if (uri.getPath().startsWith("/receive")) {
        return new ReceiveController();
      }
      if (uri.getPath().startsWith("/delete")) {
        return new DeleteController();
      }
      throw new RuntimeException("Unsupported uri: " + uri);
    } catch (URISyntaxException e) {
      throw new RuntimeException("Unable to handle request.", e);
    }
  }
}
