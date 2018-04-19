package net.consensys.orion.api.network;

import javax.net.ssl.TrustManagerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.net.TrustOptions;

public class TrustManagerFactoryWrapper implements TrustOptions {

  private final TrustManagerFactory trustManagerFactory;

  public TrustManagerFactoryWrapper(TrustManagerFactory trustManagerFactory) {
    this.trustManagerFactory = trustManagerFactory;
  }

  @Override
  public TrustOptions clone() {
    return new TrustManagerFactoryWrapper(trustManagerFactory);
  }

  @Override
  public TrustManagerFactory getTrustManagerFactory(Vertx vertx) {
    return trustManagerFactory;
  }
}
