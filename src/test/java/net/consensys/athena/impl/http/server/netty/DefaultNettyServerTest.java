package net.consensys.athena.impl.http.server.netty;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;

import net.consensys.athena.api.cmd.AthenaRouter;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;

import org.junit.Test;

public class DefaultNettyServerTest {

  @Test
  public void testStartWillStartTheServerAndListenOnHttpPortFromSettings()
      throws IOException, InterruptedException {
    int port = getPortWithNothingRunningOnIt();
    NettySettings settings = new NettySettings(empty(), of(port), empty(), new AthenaRouter());
    NettyServer server = new DefaultNettyServer(settings);
    server.start();
    URL url = new URL("http://localhost:" + port + "/upcheck");
    HttpURLConnection con = (HttpURLConnection) url.openConnection();
    con.setDoOutput(true);

    con.setRequestMethod("GET");
    DataOutputStream out = new DataOutputStream(con.getOutputStream());
    out.flush();
    out.close();
    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
    String inputLine = in.readLine();
    assertEquals("I'm up!", inputLine);
  }

  private int getPortWithNothingRunningOnIt() throws IOException {
    ServerSocket socket = new ServerSocket(0);
    int port = socket.getLocalPort();
    socket.close();
    return port;
  }
}
