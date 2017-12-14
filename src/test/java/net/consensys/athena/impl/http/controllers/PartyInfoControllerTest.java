package net.consensys.athena.impl.http.controllers;

import static org.junit.Assert.*;

import net.consensys.athena.api.network.PartyInfo;
import net.consensys.athena.impl.config.MemoryConfig;
import net.consensys.athena.impl.http.helpers.HttpTester;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Result;
import net.consensys.athena.impl.network.MemoryPartyInfo;

import java.net.URL;
import java.util.Optional;

import io.netty.handler.codec.http.HttpMethod;
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

    PartyInfo partyInfo = new MemoryPartyInfo(config);
    Controller controller = new PartyInfoController(partyInfo);

    Result result =
        new HttpTester(controller).uri("/partyinfo").method(HttpMethod.POST).sendRequest();

    assertEquals(result.getStatus().code(), HttpResponseStatus.OK.code());
    assertEquals(Optional.of(partyInfo), result.getPayload());
  }
}
