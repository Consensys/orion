//package net.consensys.athena.api.cmd;
//
//import static org.junit.Assert.*;
//
//import net.consensys.athena.impl.config.MemoryConfig;
//import DeleteHandler;
//import PartyInfoHandler;
//import PushHandler;
//import ReceiveHandler;
//import ResendHandler;
//import SendHandler;
//import UpcheckHandler;
//import net.consensys.athena.impl.utils.Serializer;
//import net.consensys.athena.impl.http.server.Controller;
//import net.consensys.athena.impl.network.MemoryNetworkNodes;
//
//import java.util.Arrays;
//import java.util.Collection;
//
//import io.netty.handler.codec.http.DefaultFullHttpRequest;
//import io.netty.handler.codec.http.HttpMethod;
//import io.netty.handler.codec.http.HttpRequest;
//import io.netty.handler.codec.http.HttpVersion;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.junit.runners.Parameterized;
//
//@RunWith(Parameterized.class)
//public class AthenaRouterTest {
//
//  private final String testName;
//  private final String path;
//  private final Class controllerClass;
//  AthenaRouter router =
//      new AthenaRouter(new MemoryNetworkNodes(), new MemoryConfig(), new Serializer());
//
//  @Parameterized.Parameters
//  public static Collection routes() {
//    //see the controllers for a brief description of what the controller does.
//    return Arrays.asList(
//        new Object[][] {
//          {"Upcheck", "/upcheck", UpcheckHandler.class},
//          {"SendRaw", "/sendraw", SendHandler.class},
//          {"ReceiveRaw", "/receiveraw", ReceiveHandler.class},
//          {"Send", "/send", SendHandler.class},
//          {"Receive", "/receive", ReceiveHandler.class},
//          {"Delete", "/delete", DeleteHandler.class},
//          {"Resend", "/resend", ResendHandler.class},
//          {"NetworkNodes", "/partyinfo", PartyInfoHandler.class},
//          {"Push", "/push", PushHandler.class},
//        });
//  }
//
//  public AthenaRouterTest(String testName, String path, Class controllerClass) {
//
//    this.testName = testName;
//    this.path = path;
//    this.controllerClass = controllerClass;
//  }
//
//  @Test
//  public void testRouter() {
//    HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path);
//    Controller controller = router.lookup(request);
//    assertEquals(
//        testName + "expected " + path + " to go to " + controllerClass,
//        controllerClass,
//        controller.getClass());
//  }
//
//  //TODO Check default content type for send and receive.
//}
