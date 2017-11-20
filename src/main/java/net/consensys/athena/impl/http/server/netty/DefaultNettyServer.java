package net.consensys.athena.impl.http.server.netty;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerDomainSocketChannel;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

public class DefaultNettyServer implements NettyServer {
  private NettySettings settings;

  public DefaultNettyServer(NettySettings settings) {
    this.settings = settings;
  }

  private List<Channel> channels;
  private EventLoopGroup bossLoop;

  private EventLoopGroup workerLoop;

  private DefaultEventExecutorGroup executor;

  @Override
  public void start() {
    bossLoop = createEventLoop(settings.bossThreads(), "Boss");
    int workerThreads = settings.workerThreads();
    if (workerThreads > 0) {
      workerLoop = createEventLoop(workerThreads, "Worker");
    } else {
      workerLoop = bossLoop;
    }
    List<ChannelFuture> futures = new ArrayList<>();
    if (settings.isHttp()) {
      futures.add(buildHttpChannel());
    }

    if (settings.isHttps()) {
      futures.add(buildHttpsChannel());
    }

    if (settings.isDomainSocket()) {
      futures.add(buildDomainSocketChannel());
    }

    channels =
        futures
            .parallelStream()
            .map(
                channelFuture -> {
                  try {
                    return channelFuture.sync().channel();
                  } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                  }
                })
            .collect(Collectors.toList());
  }

  @Override
  public void join() throws InterruptedException {
    channels
        .parallelStream()
        .forEach(
            channel -> {
              try {
                channel.closeFuture().sync();
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
            });
  }

  private EventLoopGroup createEventLoop(int threads, String name) {
    if (settings.isKQueue()) {
      return new KQueueEventLoopGroup(threads, new DefaultThreadFactory("kqueue-" + name, false));
    } else if (settings.isEpoll()) {
      return new EpollEventLoopGroup(threads, new DefaultThreadFactory("epoll-" + name, false));
    } else {
      return new NioEventLoopGroup(threads, new DefaultThreadFactory("nio-" + name, false));
    }
  }

  private ChannelFuture buildDomainSocketChannel() {
    ServerBootstrap bootstrap;
    if (settings.isKQueue()) {
      bootstrap = initChannel(KQueueServerDomainSocketChannel.class);
    } else if (settings.isEpoll()) {
      bootstrap = initChannel(EpollServerDomainSocketChannel.class);
    } else {
      throw new IllegalStateException(
          "Unable to support domain sockets in the current enfironment, as neither epoll or kqueue "
              + "are working. See http://netty.io/wiki/native-transports.html for more.");
    }
    SocketAddress localAddress = new DomainSocketAddress(settings.getDomainSocketPath().get());
    return bootstrap.bind(localAddress);
  }

  private ChannelFuture buildHttpsChannel() {
    throw new IllegalStateException("HTTPS support is coming, but not yet implemented");
  }

  private ChannelFuture buildHttpChannel() {
    ServerBootstrap bootstrap;
    if (settings.isKQueue()) {
      bootstrap = initChannel(KQueueServerSocketChannel.class);
    } else if (settings.isEpoll()) {
      bootstrap = initChannel(EpollServerSocketChannel.class);
    } else {
      bootstrap = initChannel(NioServerSocketChannel.class);
    }

    return bootstrap.bind(settings.httpPort.get());
  }

  @Override
  public void stop() {}

  private ServerBootstrap initChannel(Class<? extends ServerChannel> channelClass) {
    ServerBootstrap bootstrap = new ServerBootstrap();
    final RequestDispatcher requestDispatcher = new RequestDispatcher(settings.getRouter());
    ChannelInitializer<SocketChannel> childHandler =
        new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(final SocketChannel ch) throws Exception {
            ch.pipeline().addLast("codec", new HttpServerCodec());
            ch.pipeline().addLast("aggregator", new HttpObjectAggregator(512 * 1024));
            ch.pipeline().addLast("request", new RequestDispatcherHandler(requestDispatcher));
          }
        };
    return bootstrap
        .group(bossLoop, workerLoop)
        .channel(channelClass)
        .handler(new LoggingHandler(NettyServer.class, LogLevel.DEBUG))
        .childHandler(childHandler);
  }
}
