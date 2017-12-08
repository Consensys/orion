package net.consensys.athena.api.cmd;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import net.consensys.athena.impl.http.server.netty.DefaultNettyServer;
import net.consensys.athena.impl.http.server.netty.NettyServer;
import net.consensys.athena.impl.http.server.netty.NettySettings;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Athena {

  private static final int DEFAULT_HTTP_PORT = 8080;

  public static void main(String[] args) throws InterruptedException {
    // start http server
    Optional<String> socketPath = empty();
    NettySettings settings =
        new NettySettings(
            socketPath, of(DEFAULT_HTTP_PORT), empty(), new AthenaRouter(), new ObjectMapper());
    NettyServer server = new DefaultNettyServer(settings);
    server.start();
    server.join();
  }
}
