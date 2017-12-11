package net.consensys.athena.api.cmd;

import static java.util.Optional.empty;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.impl.config.TomlConfigBuilder;
import net.consensys.athena.impl.http.server.Serializer;
import net.consensys.athena.impl.http.server.netty.DefaultNettyServer;
import net.consensys.athena.impl.http.server.netty.NettyServer;
import net.consensys.athena.impl.http.server.netty.NettySettings;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Athena {

  private static final int DEFAULT_HTTP_PORT = 8080;

  public static void main(String[] args) throws InterruptedException {
    // start http server
    Optional<String> socketPath = empty();

    InputStream configAsStream =
        MethodHandles.lookup().lookupClass().getResourceAsStream("sample.conf");
    TomlConfigBuilder configBuilder = new TomlConfigBuilder();

    Config config = configBuilder.build(configAsStream);

    /*
     Optional<String> domainSocketPath,
     Optional<Integer> httpPort,
     Optional<Integer> httpsPort,

     of(DEFAULT_HTTP_PORT),

    */

    NettySettings settings =
        new NettySettings(
            socketPath,
            Optional.of((int) config.port()),
            empty(),
            new AthenaRouter(),
            new Serializer(new ObjectMapper()));
    NettyServer server = new DefaultNettyServer(settings);
    server.start();
    server.join();
  }
}
