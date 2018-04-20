package net.consensys.orion.api.network;


import net.consensys.orion.api.cmd.OrionStartException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Optional;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.google.common.hash.Hashing;
import com.google.common.net.InternetDomainName;
import io.netty.handler.ssl.util.SimpleTrustManagerFactory;
import io.netty.util.internal.EmptyArrays;
import io.vertx.core.Vertx;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.TrustOptions;
import sun.security.x509.X500Name;

public class HostAndFingerprintTrustManagerFactory extends SimpleTrustManagerFactory {

  public static HostAndFingerprintTrustManagerFactory tofu(HostFingerprintRepository repository) {
    return new HostAndFingerprintTrustManagerFactory(repository, false, true, Optional.empty());
  }

  public static HostAndFingerprintTrustManagerFactory whitelist(HostFingerprintRepository repository) {
    return new HostAndFingerprintTrustManagerFactory(repository, false, false, Optional.empty());
  }

  public static HostAndFingerprintTrustManagerFactory caOrTofu(
      HostFingerprintRepository repository,
      TrustOptions caTrustOptions,
      Vertx vertx) {
    TrustManagerFactory tmf = null;
    try {
      tmf = caTrustOptions.getTrustManagerFactory(vertx);
    } catch (Exception e) {
      throw new OrionStartException("Error initializing the CA trust manager factory", e);
    }
    return new HostAndFingerprintTrustManagerFactory(repository, false, true, Optional.of(tmf));
  }

  public static HostAndFingerprintTrustManagerFactory caOrTofuDefaultJDKTruststore(
      HostFingerprintRepository repository,
      Vertx vertx) {


    JksOptions delegateTrustOptions = new JksOptions();

    if (System.getProperty("javax.net.ssl.trustStore") != null) {
      delegateTrustOptions.setPath(System.getProperty("javax.net.ssl.trustStore"));
      if (System.getProperty("javax.net.ssl.trustStorePassword") != null) {
        delegateTrustOptions.setPassword(System.getProperty("javax.net.ssl.trustStorePassword"));
      }
    } else {
      Path jsseCaCerts = Paths.get(System.getProperty("java.home"), "lib", "security", "jssecacerts");
      if (jsseCaCerts.toFile().exists()) {
        delegateTrustOptions.setPath(jsseCaCerts.toString());
      } else {
        Path cacerts = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
        delegateTrustOptions.setPath(cacerts.toString());
      }
      delegateTrustOptions.setPassword("changeit");
    }
    return caOrTofu(repository, delegateTrustOptions, vertx);
  }

  public static HostAndFingerprintTrustManagerFactory insecure(HostFingerprintRepository repository) {
    return new HostAndFingerprintTrustManagerFactory(repository, true, true, Optional.empty());
  }

  private final boolean addNewCerts;
  private final boolean insecure;
  private final Optional<TrustManagerFactory> delegate;
  private HostFingerprintRepository repository;

  private HostAndFingerprintTrustManagerFactory(
      HostFingerprintRepository repository,
      boolean insecure,
      boolean addNewCerts,
      Optional<TrustManagerFactory> delegate) {
    this.repository = repository;
    this.insecure = insecure;
    this.addNewCerts = addNewCerts;
    this.delegate = delegate;
  }

  private final TrustManager tm = new X509TrustManager() {

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
      checkTrusted(x509Certificates, s, true);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
      checkTrusted(x509Certificates, s, false);
    }

    public void checkTrusted(X509Certificate[] x509Certificates, String s, boolean client) throws CertificateException {
      X509Certificate cert = x509Certificates[0];
      try {

        byte[] fingerprint = Hashing.sha1().hashBytes(cert.getEncoded()).asBytes();
        String peerHostname = ((X500Name) cert.getSubjectDN()).getCommonName();
        if (!repository.contains(peerHostname) && addNewCerts && InternetDomainName.isValid(peerHostname)) {
          repository.addHostFingerprint(peerHostname, fingerprint);
        }
        if (!repository.contains(peerHostname, fingerprint)) {
          if (insecure) {
            if (addNewCerts && InternetDomainName.isValid(peerHostname)) {
              repository.addHostFingerprint(peerHostname, fingerprint);
            }
          } else {
            boolean passesDelegate = false;
            if (delegate.isPresent()) {
              for (TrustManager trustManager : delegate.get().getTrustManagers()) {
                if (trustManager instanceof X509TrustManager) {
                  if (client) {
                    ((X509TrustManager) trustManager).checkClientTrusted(x509Certificates, s);
                  } else {
                    ((X509TrustManager) trustManager).checkServerTrusted(x509Certificates, s);
                  }
                  passesDelegate = true;
                }
              }
            }
            if (!passesDelegate) {
              throw new CertificateException("Certificate with unknown fingerprint: " + cert.getSubjectDN());
            }
          }

        }
      } catch (IOException e) {
        throw new CertificateException("Invalid certificate " + cert.getSubjectDN());
      }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return EmptyArrays.EMPTY_X509_CERTIFICATES;
    }
  };

  @Override
  protected void engineInit(KeyStore keyStore) throws Exception {
    if (delegate.isPresent()) {
      delegate.get().init(keyStore);
    }
  }

  @Override
  protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws Exception {
    if (delegate.isPresent()) {
      delegate.get().init(managerFactoryParameters);
    }
  }

  @Override
  protected TrustManager[] engineGetTrustManagers() {
    return new TrustManager[] {tm};
  }
}
