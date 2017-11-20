package net.consensys.athena.api.cmd;

import static org.junit.Assert.*;

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

import java.util.Arrays;
import java.util.Collection;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class AthenaRouterTest {

  private final String testName;
  private final String path;
  private final Class controllerClass;
  AthenaRouter router = new AthenaRouter();

  @Parameterized.Parameters
  public static Collection routes() {
    return Arrays.asList(
        new Object[][] {
          {"Upcheck", "/upcheck", UpcheckController.class},
          {"SendRaw", "/sendraw", SendRawController.class},
          {"ReceiveRaw", "/receiveraw", ReceiveRawController.class},
          {"Send", "/send", SendController.class},
          {"Receive", "/receive", ReceiveController.class},
          {"Delete", "/delete", DeleteController.class},
          {"Resend", "/resend", ResendController.class},
          {"PartyInfo", "/partyinfo", PartyInfoController.class},
          {"Push", "/push", PushController.class},
        });
  }

  public AthenaRouterTest(String testName, String path, Class controllerClass) {

    this.testName = testName;
    this.path = path;
    this.controllerClass = controllerClass;
  }

  @Test
  public void testRouter() {
    HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path);
    Controller controller = router.lookup(request);
    assertEquals(
        testName + "expected " + path + " to go to " + controllerClass,
        controllerClass,
        controller.getClass());
  }
}
