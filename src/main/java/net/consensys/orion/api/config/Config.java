package net.consensys.orion.api.config;

import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

/** Configuration for Orion. Refer to the "sample.conf" file for documentation on config elements */
public interface Config {

  /**
   * Externally accessible URL for this node's public API This is what is advertised to other nodes on the network and
   * must be reachable by them.
   *
   * @return URL for this nodes public API
   */
  URL url();

  /**
   * Port to listen on for the public API.
   *
   * @return Port to listen on for the public API
   */
  int port();

  /**
   * Internally accessible URL for this node's public API This is what is advertised to other nodes on the network and
   * must be reachable by them.
   *
   * @return URL for this nodes private API
   */
  URL privacyUrl();

  /**
   * Port to listen on for the private API.
   *
   * @return Port to listen on for the private API
   */
  int privacyPort();

  /**
   * Path to the lib sodium shared library.
   *
   * <p>
   * <strong>Default:</strong>
   *
   * <ul>
   * <li><b>Linux</b> /usr/local/lib/libsodium.so
   * <li><b>Mac</b> /usr/local/lib/libsodium.dylib
   * <li><b>Windows</b> C:/libsodium/libsodium.dll
   * </ul>
   *
   * @return Path to the lib sodium shared library.
   */
  String libSodiumPath();

  /**
   * Directory to which all other paths referenced in the config are relative to.
   *
   * <p>
   * <strong>Default:</strong> The current directory
   *
   * @return Working directory to use
   */
  Path workDir();

  /**
   * Path to the socket for use in the private API / IPC. NB. If this isn't set, the private API will not be accessible.
   *
   * @return Path to IPC socket for private API access
   */
  Optional<Path> socket();

  /**
   * Initial list of other nodes in the network. Orion will automatically connect to other nodes not in this list that
   * are advertised by the nodes below, so these can be considered the "boot nodes."
   *
   * <p>
   * <strong>Default:</strong> []
   *
   * @return Array of other node URLs to connect to on startup
   */
  URL[] otherNodes();

  /**
   * The set of public keys this node will host.
   *
   * <p>
   * <strong>Default:</strong> []
   *
   * @return Array of paths to public keys
   */
  Path[] publicKeys();

  /**
   * The corresponding set of private keys. These must correspond to the public keys listed <i>publicKeys</i>.
   *
   * <p>
   * <strong>Default:</strong> []
   *
   * @see #publicKeys()
   * @return Array of paths to corresponding private keys
   */
  Path[] privateKeys();

  /**
   * Optional comma-separated list of paths to public keys to add as recipients for every transaction sent through this
   * node, e.g. for backup purposes. These keys must be advertised by some Orion node on the network, i.e. be in a
   * node's <i>publicKeys/privateKeys</i> lists.
   *
   * <p>
   * <strong>Default:</strong> []
   *
   * @see #publicKeys()
   * @see #privateKeys()
   * @return Array of paths to public keys that are always included as recipients
   */
  Path[] alwaysSendTo();

  /**
   * Optional file containing the passwords needed to unlock the given <i>privateKeys</i> The file should contain one
   * password per line -- add an empty line if any one key isn't locked.
   *
   * @see #privateKeys()
   * @return A file containing the passwords for the specified privateKeys
   */
  Optional<Path> passwords();

  /**
   * Storage engine used to save payloads and related information. Options:
   *
   * <ul>
   * <li>leveldb:path - LevelDB
   * <li>mapdb:path - MapDB
   * <li>memory - Contents are cleared when Orion exits
   * </ul>
   *
   * <strong>Default:</strong> "leveldb"
   *
   * @return Storage string specifying a storage engine and/or storage path
   */
  String storage();

  /**
   * Optional IP whitelist for the public API. If unspecified/empty, connections from all sources will be allowed (but
   * the private API remains accessible only via the IPC socket.) To allow connections from localhost when a whitelist
   * is defined, e.g. when running multiple Orion nodes on the same machine, add "127.0.0.1" and "::1" to this list.
   *
   * <p>
   * <strong>Default:</strong> []
   *
   * @see #socket()
   * @return Array of IPv4 and IPv6 addresses that may connect to this node's public API
   */
  String[] ipWhitelist();

  /**
   * TLS status. Options:
   *
   * <ul>
   * <li><strong>strict:</strong> All connections to and from this node must use TLS with mutual authentication. See the
   * documentation for tlsServerTrust and tlsClientTrust
   * <li><strong>off:</strong> Mutually authenticated TLS is not used for in- and outbound connections, although
   * unauthenticated connections to HTTPS hosts are still possible. This should only be used if another transport
   * security mechanism like WireGuard is in place.
   * </ul>
   *
   * <strong>Default:</strong> "strict"
   *
   * @see #tlsServerTrust()
   * @see #tlsClientTrust()
   * @return TLS status
   */
  String tls();

  /**
   * File containing the server's TLS certificate in Apache format. This is used to identify this node to other nodes in
   * the network when they connect to the public API. If it doesn't exist it will be created.
   *
   * <p>
   * <strong>Default:</strong> "tls-server-cert.pem"
   *
   * @return TLS certificate file to use for the public API
   */
  Path tlsServerCert();

  /**
   * List of files that constitute the CA trust chain for the server certificate. This can be empty for
   * auto-generated/non-PKI-based certificates.
   *
   * <p>
   * <strong>Default:</strong> []
   *
   * @return Array of TLS chain certificates to use for the public API
   */
  Path[] tlsServerChain();

  /**
   * The private key file for the server TLS certificate. If the file doesn't exist it will be created.
   *
   * <p>
   * <strong>Default:</strong> "tls-server-key.pem"
   *
   * @return TLS key to use for the public API
   */
  Path tlsServerKey();

  /**
   * TLS trust mode for the server. This decides who's allowed to connect to it. Options:
   *
   * <ul>
   * <li><strong>whitelist:</strong> Only nodes that have previously connected to this node and been added to the
   * <i>tlsKnownClients</i> file will be allowed to connect. This mode will not add any new clients to the
   * <i>tlsKnownClients</i> file.
   * <li><strong>tofu:</strong> (Trust-on-first-use) Only the first node that connects identifying as a certain host
   * will be allowed to connect as the same host in the future. Note that nodes identifying as other hosts will still be
   * able to connect - switch to whitelist after populating the <i>tlsKnownClients</i> list to restrict access.
   * <li><strong>ca:</strong> Only nodes with a valid certificate and chain of trust to one of the system root
   * certificates will be allowed to connect. The folder containing trusted root certificates can be overriden with the
   * SYSTEM_CERTIFICATE_PATH environment variable.
   * <li><strong>ca-or-tofu:</strong> A combination of ca and tofu: If a certificate is valid, it is always allowed and
   * added to the <i>tlsKnownClients</i> list. If it is self-signed, it will be allowed only if it's the first
   * certificate this node has seen for that host.
   * <li><strong>insecure-no-validation:</strong> Any client can connect, however they will still be added to the
   * <i>tlsKnownClients</i> file.
   * </ul>
   *
   * <strong>Default:</strong> "tofu"
   *
   * @see #tlsKnownClients()
   * @return TLS server trust mode
   */
  String tlsServerTrust();

  /**
   * TLS known clients file for the server. This contains the fingerprints of public keys of other nodes that are
   * allowed to connect to this one for the ca-or-tofu, tofu and whitelist trust modes
   *
   * <p>
   * <strong>Default:</strong> "tls-known-clients"
   *
   * @see #tlsServerTrust()
   * @return TLS server known clients file
   */
  Path tlsKnownClients();

  /**
   * File containing the client's TLS certificate in Apache format. This is used to identify this node to other nodes in
   * the network when it is connecting to their public APIs. If it doesn't exist it will be created
   *
   * <p>
   * <strong>Default:</strong> "tls-client-cert.pem"
   *
   * @return TLS client certificate file
   */
  Path tlsClientCert();

  /**
   * List of files that constitute the CA trust chain for the client certificate. This can be empty for
   * auto-generated/non-PKI-based certificates.
   *
   * <p>
   * <strong>Default:</strong> []
   *
   * @return Array of TLS chain certificates to use for connections to other nodes
   */
  Path[] tlsClientChain();

  /**
   * The private key file for the client TLS certificate. If it doesn't exist it will be created.
   *
   * <p>
   * <strong>Default:</strong> "tls-client-key.pem"
   *
   * @return TLS key to use for connections to other nodes
   */
  Path tlsClientKey();

  /**
   * TLS trust mode for the client. This decides which servers it will connect to. Options:
   *
   * <ul>
   * <li><strong>whitelist:</strong> This node will only connect to servers it has previously seen and added to the
   * <i>tlsKnownServers</i> file. This mode will not add any new servers to the <i>tlsKnownServers</i> file.
   * <li><strong>tofu:</strong> (Trust-on-first-use) This node will only connect to the same server for any given host.
   * (Similar to how OpenSSH works.)
   * <li><strong>ca:</strong> The node will only connect to servers with a valid certificate and chain of trust to one
   * of the system root certificates. The folder containing trusted root certificates can be overriden with the
   * SYSTEM_CERTIFICATE_PATH environment variable.
   * <li><strong>ca-or-tofu:</strong> A combination of ca and tofu: If a certificate is valid, it is always allowed and
   * added to the <i>tlsKnownServers</i> list. If it is self-signed, it will be allowed only if it's the first
   * certificate this node has seen for that host.
   * <li><strong>insecure-no-validation:</strong> This node will connect to any server, regardless of certificate,
   * however it will still be added to the <i>tlsKnownServers</i> file.
   * </ul>
   *
   * <strong>Default:</strong> "ca-or-tofu"
   *
   * @see #tlsKnownServers()
   * @return TLS client trust mode
   */
  String tlsClientTrust();

  /**
   * TLS known servers file for the client. This contains the fingerprints of public keys of other nodes that this node
   * has encountered for the ca-or-tofu, tofu and whitelist trust modes.
   *
   * <p>
   * <strong>Default:</strong> "tls-known-servers"
   *
   * @see #tlsKnownServers()
   * @return TLS client known servers file
   */
  Path tlsKnownServers();

  /**
   * Verbosity level (each level includes all prior levels)
   *
   * <ul>
   * <li>0: Only fatal errors
   * <li>1: Warnings
   * <li>2: Informational messages
   * <li>3: Debug messages
   * </ul>
   *
   * <strong>Default:</strong> 1
   *
   * @return Level of verbosity, that is print more detailed information
   */
  long verbosity();

  /** @return Array of key pair names to generate */
  Optional<String[]> generateKeys();

  /** @return Output current version information flag */
  Optional<Boolean> showVersion();
}
