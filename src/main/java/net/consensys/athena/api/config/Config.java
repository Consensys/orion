package net.consensys.athena.api.config;

import java.io.File;
import java.util.Optional;

public interface Config {

  /**
   * @return URL for this node (what's advertised to other nodes, e.g.
   *     https://constellation.mydomain.com/)
   */
  String url();

  /** @return Port to listen on for the public API */
  long port();

  /**
   * @return Working directory to use (relative paths specified for other options are relative to
   *     the working directory)
   */
  Optional<File> workDir();

  /** @return Path to IPC socket file to create for private API access */
  Optional<File> socket();

  /** @return Array of other node URLs to connect to on startup (this list may be incomplete) */
  File[] otherNodes();

  /** @return Array of paths to public keys to advertise */
  File[] publicKeys();

  /**
   * @return Array of paths to corresponding private keys (these must be given in the same order as
   *     --publickeys)
   */
  File[] privateKeys();

  /**
   * @return Array of paths to public keys that are always included as recipients (these must be
   *     advertised somewhere)
   */
  File[] alwaysSendTo();

  /**
   * @return A file containing the passwords for the specified --privatekeys, one per line, in the
   *     same order (if one key is not locked, add an empty line)
   */
  Optional<File> passwords();

  /** @return Storage string specifying a storage engine and/or storage path */
  String storage();

  /** @return Array of IPv4 and IPv6 addresses that may connect to this node's public API */
  String[] ipWhitelist();

  /** @return TLS status (strict, off) */
  String tls();

  /** @return TLS certificate file to use for the public API */
  File tlsServerCert();

  /** @return Array of TLS chain certificates to use for the public API */
  File[] tlsServerChain();

  /** @return TLS key to use for the public API */
  File tlsServerKey();

  /** @return TLS server trust mode (whitelist, ca-or-tofu, ca, tofu, insecure-no-validation) */
  String tlsServerTrust();

  /** @return TLS server known clients file for the ca-or-tofu, tofu and whitelist trust modes */
  File tlsKnownClients();

  /** @return TLS client certificate file to use for connections to other nodes */
  File tlsClientCert();

  /** @return Array of TLS chain certificates to use for connections to other nodes */
  File[] tlsClientChain();

  /** @return TLS key to use for connections to other nodes */
  File tlsClientKey();

  /** @return TLS client trust mode (whitelist, ca-or-tofu, ca, tofu, insecure-no-validation) */
  String tlsClientTrust();

  /** @return TLS client known servers file for the ca-or-tofu, tofu and whitelist trust modes */
  File tlsKnownServers();

  /** @return Array of key pair names to generate */
  Optional<String[]> generateKeys();

  /** @return Output current version information flag */
  Optional<Boolean> showVersion();

  /** @return Level of verbosity, that is print more detailed information */
  long verbosity();
}
