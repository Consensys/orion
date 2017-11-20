package net.consensys.athena.impl.http.server.netty;

public interface NettyServer {
  void start() throws InterruptedException;

  void join() throws InterruptedException;

  void stop();
}
