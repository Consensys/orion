package net.consensys.athena.impl.http.server;

import net.consensys.athena.impl.http.data.Serializer;

import java.io.File;
import java.util.Optional;

import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;

public class HttpServerSettings {

  private Optional<Integer> httpPort;
  private Optional<File> domainSocketPath;
  private Optional<Integer> httpsPort;

  // legacy ?
  private static final int DEFAULT_BOSS_THREAD_COUNT = 1;
  private static final int DEFAULT_WORKER_THREAD_COUNT = 4;
  private final Serializer serializer;

  public HttpServerSettings(
      Optional<File> domainSocketPath,
      Optional<Integer> httpPort,
      Optional<Integer> httpsPort,
      Serializer serializer) {
    this.httpPort = httpPort;
    this.domainSocketPath = domainSocketPath;
    this.httpsPort = httpsPort;
    this.serializer = serializer;
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

  public Optional<File> getDomainSocketPath() {
    return domainSocketPath;
  }

  public int bossThreads() {
    return DEFAULT_BOSS_THREAD_COUNT;
  }

  public int workerThreads() {
    return DEFAULT_WORKER_THREAD_COUNT;
  }
}
