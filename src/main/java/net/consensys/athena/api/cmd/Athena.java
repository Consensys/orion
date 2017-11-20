package net.consensys.athena.api.cmd;

import net.consensys.athena.impl.http.server.netty.DefaultNettyServer;
import net.consensys.athena.impl.http.server.netty.NettyServer;
import net.consensys.athena.impl.http.server.netty.NettySettings;

import java.util.Optional;

public class Athena {
  public static void main(String[] args) throws InterruptedException {
    //    Optional<String> socketPath = Optional.of("/Users/rob/athena.ipc");
    Optional<String> socketPath = Optional.empty();
    NettySettings settings =
        new NettySettings(socketPath, Optional.of(8080), Optional.empty(), new AthenaRouter());
    NettyServer server = new DefaultNettyServer(settings);
    server.start();
    server.join();
  }
}
