package net.consensys.athena.impl.http.server.netty;

import net.consensys.athena.impl.http.server.Router;

import java.util.Optional;

import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;

public class NettySettings {
  public Optional<Integer> httpPort;
  public Optional<String> domainSocketPath;
  public Optional<Integer> httpsPort;
  private Router router;

  public NettySettings(
      Optional<String> domainSocketPath,
      Optional<Integer> httpPort,
      Optional<Integer> httpsPort,
      Router router) {
    this.httpPort = httpPort;
    this.domainSocketPath = domainSocketPath;
    this.httpsPort = httpsPort;
    this.router = router;
  }

  public boolean isHttp() {
    return httpPort.isPresent();
  }

  public boolean isHttps() {
    return httpsPort.isPresent();
  }

  public boolean isDomainSocket() {
    return domainSocketPath.isPresent();
  }

  public boolean isEpoll() {
    return Epoll.isAvailable();
  }

  public boolean isKQueue() {
    return KQueue.isAvailable();
  }

  public Optional<Integer> getHttpPort() {
    return httpPort;
  }

  public Optional<Integer> getHttpsPort() {
    return httpsPort;
  }

  public Optional<String> getDomainSocketPath() {
    return domainSocketPath;
  }

  public int bossThreads() {
    return 1;
  }

  public int workerThreads() {
    return 4;
  }

  public Router getRouter() {
    return router;
  }
}
