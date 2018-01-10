package net.consensys.athena.impl.http.server.netty;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.consensys.athena.impl.http.server.HttpServerSettings;
import net.consensys.athena.impl.utils.Serializer;

import java.io.File;

import org.junit.Test;

public class HttpServerSettingsTest {

  @Test
  public void testIsHttpIfHttpPortSet() {
    HttpServerSettings settings =
        new HttpServerSettings(empty(), of(8080), empty(), new Serializer());
    assertFalse(settings.isDomainSocket());
    assertTrue(settings.isHttp());
    assertFalse(settings.isHttps());
  }

  @Test
  public void testIsHttpsIfHttpsPortSet() {
    HttpServerSettings settings =
        new HttpServerSettings(empty(), empty(), of(8080), new Serializer());
    assertFalse(settings.isDomainSocket());
    assertFalse(settings.isHttp());
    assertTrue(settings.isHttps());
  }

  @Test
  public void testIsDomainIfDomainSocketPathSet() {
    HttpServerSettings settings =
        new HttpServerSettings(of(new File("/tmp/mysock.ipc")), empty(), empty(), new Serializer());
    assertTrue(settings.isDomainSocket());
    assertFalse(settings.isHttp());
    assertFalse(settings.isHttps());
  }
}
