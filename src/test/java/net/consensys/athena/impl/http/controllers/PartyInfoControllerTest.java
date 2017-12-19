package net.consensys.athena.impl.http.controllers;

import static junit.framework.TestCase.assertEquals;

import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.impl.config.MemoryConfig;
import net.consensys.athena.impl.http.data.RequestImpl;
import net.consensys.athena.impl.http.data.Result;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.network.MemoryNetworkNodes;

import java.net.URL;
import java.util.Optional;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Test;

public class PartyInfoControllerTest {
  @Test
  public void testSuccessfullProcessingOfRequest() throws Exception {

    MemoryConfig config = new MemoryConfig();

    config.setUrl(new URL("http://127.0.0.1:9001/"));
    URL[] otherNodes = new URL[1];
    otherNodes[0] = new URL("http://127.0.0.1:9000/");
    config.setOtherNodes(otherNodes);

    NetworkNodes networkNodes = new MemoryNetworkNodes(config);
    Controller controller = new PartyInfoController(networkNodes);

    Result result = controller.handle(new RequestImpl(Optional.empty()));

    assertEquals(result.getStatus().code(), HttpResponseStatus.OK.code());
    assertEquals(Optional.of(networkNodes), result.getPayload());
  }
}
