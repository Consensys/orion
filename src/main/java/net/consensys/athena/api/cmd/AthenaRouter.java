package net.consensys.athena.api.cmd;

import net.consensys.athena.impl.http.controllers.DeleteController;
import net.consensys.athena.impl.http.controllers.PartyInfoController;
import net.consensys.athena.impl.http.controllers.PushController;
import net.consensys.athena.impl.http.controllers.ReceiveController;
import net.consensys.athena.impl.http.controllers.ReceiveRawController;
import net.consensys.athena.impl.http.controllers.ResendController;
import net.consensys.athena.impl.http.controllers.SendController;
import net.consensys.athena.impl.http.controllers.SendRawController;
import net.consensys.athena.impl.http.controllers.UpcheckController;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Router;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Optional;

import io.netty.handler.codec.http.HttpRequest;

public class AthenaRouter implements Router {
  private static final LinkedHashMap<String, Controller> ROUTES = new LinkedHashMap();

  static {
    ROUTES.put("/upcheck", UpcheckController.INSTANCE);
    ROUTES.put("/sendraw", SendRawController.INSTANCE);
    ROUTES.put("/receiveraw", ReceiveRawController.INSTANCE);
    ROUTES.put("/send", SendController.INSTANCE);
    ROUTES.put("/receive", ReceiveController.INSTANCE);
    ROUTES.put("/delete", DeleteController.INSTANCE);
    ROUTES.put("/resend", ResendController.INSTANCE);
    ROUTES.put("/partyinfo", PartyInfoController.INSTANCE);
    ROUTES.put("/push", PushController.INSTANCE);
  }

  @Override
  public Controller lookup(HttpRequest request) {
    try {
      URI uri = new URI(request.uri());
      Optional<Entry<String, Controller>> route =
          ROUTES
              .entrySet()
              .stream()
              .filter(entry -> uri.getPath().startsWith(entry.getKey()))
              .findFirst();
      if (route.isPresent()) {
        return route.get().getValue();
      }
      throw new RuntimeException("Unsupported uri: " + uri);
    } catch (URISyntaxException e) {
      throw new RuntimeException("Unable to handle request.", e);
    }
  }
}
