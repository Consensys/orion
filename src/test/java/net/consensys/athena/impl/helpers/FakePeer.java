package net.consensys.athena.impl.helpers;

import java.io.IOException;
import java.net.URL;
import java.security.PublicKey;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class FakePeer {
  public final MockWebServer server;
  public final PublicKey publicKey;

  public FakePeer(MockResponse response, PublicKey publicKey) throws IOException {
    server = new MockWebServer();
    this.publicKey = publicKey;
    server.enqueue(response);
    server.start();
  }

  public URL getURL() {
    return server.url("").url();
  }
}
