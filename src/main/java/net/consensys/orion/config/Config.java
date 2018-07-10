/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package net.consensys.orion.config;

import static net.consensys.cava.config.ConfigurationErrors.noErrors;
import static net.consensys.cava.config.ConfigurationErrors.singleError;
import static net.consensys.cava.config.PropertyValidator.allInList;
import static net.consensys.cava.config.PropertyValidator.inRange;
import static net.consensys.cava.config.PropertyValidator.isURL;

import net.consensys.cava.config.Configuration;
import net.consensys.cava.config.ConfigurationError;
import net.consensys.cava.config.DocumentPosition;
import net.consensys.cava.config.PropertyValidator;
import net.consensys.cava.config.Schema;
import net.consensys.cava.config.SchemaBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * The configuration of Orion.
 */
public class Config {

  private static final Schema SCHEMA = configSchema();

  public static Config load(Path configFile) throws IOException {
    return load(Configuration.fromToml(configFile, SCHEMA));
  }

  public static Config load(String config) {
    return load(Configuration.fromToml(config, SCHEMA));
  }

  public static Config load(InputStream is) throws IOException {
    return load(Configuration.fromToml(is, SCHEMA));
  }

  private static Config load(Configuration configuration) {
    List<ConfigurationError> errors = configuration.errors();
    if (!errors.isEmpty()) {
      String errorString = errors.stream().limit(5).map(ConfigurationError::toString).collect(Collectors.joining("\n"));
      if (errors.size() > 5) {
        errorString += "\n...";
      }
      throw new ConfigException(errorString);
    }
    return new Config(configuration);
  }

  public static Config defaultConfig() {
    return new Config(Configuration.empty(SCHEMA));
  }

  private final Configuration configuration;
  private final Path workDir;

  private Config(Configuration configuration) {
    this.configuration = configuration;
    this.workDir = Paths.get(configuration.getString("workdir"));
  }

  /**
   * Externally accessible URL for this node's Orion API
   * <p>
   * This is what is advertised to other nodes on the network and must be reachable by them.
   *
   * @return URL for this node's Orion API
   */
  public URL nodeUrl() {
    return getURL("nodeurl");
  }

  /**
   * Port to listen on for the Orion API.
   *
   * @return Port to listen on for the Orion API
   */
  public int nodePort() {
    return configuration.getInteger("nodeport");
  }

  /**
   * Network interface to bind the Orion API to.
   *
   * @return the network interface to bind the Orion API to
   */
  public String nodeNetworkInterface() {
    return configuration.getString("nodenetworkinterface");
  }

  /**
   * URL advertised to the Ethereum client paired with this node.
   *
   * @return URL for this node's client API
   */
  public URL clientUrl() {
    return getURL("clienturl");
  }

  /**
   * Port to listen on for the client API.
   *
   * @return Port to listen on for the client API
   */
  public int clientPort() {
    return configuration.getInteger("clientport");
  }

  /**
   * Network interface to bind the client API to.
   *
   * @return the network interface to bind the client API to.
   */
  public String clientNetworkInterface() {
    return configuration.getString("clientnetworkinterface");
  }

  /**
   * Path to the lib sodium shared library.
   *
   * @return Path to the lib sodium shared library.
   */
  @Nullable
  public Path libSodiumPath() {
    if (!configuration.contains("libsodiumpath")) {
      return null;
    }
    return Paths.get(configuration.getString("libsodiumpath"));
  }

  /**
   * Directory to which all other paths referenced in the config are relative to.
   *
   * <p>
   * <strong>Default:</strong> The current directory
   *
   * @return Working directory to use
   */
  public Path workDir() {
    return workDir;
  }

  /**
   * Initial list of other nodes in the network. Orion will automatically connect to other nodes not in this list that
   * are advertised by the nodes below, so these can be considered the "boot nodes."
   *
   * <p>
   * <strong>Default:</strong> []
   *
   * @return A list of other node URLs to connect to on startup.
   */
  public List<URL> otherNodes() {
    return configuration.getListOfString("othernodes").stream().map(urlString -> {
      try {
        return new URL(urlString);
      } catch (MalformedURLException e) {
        throw new IllegalStateException("key 'othernodes' should have been validated, yet it's invalid", e);
      }
    }).collect(Collectors.toList());
  }

  /**
   * The set of public keys this node will host.
   *
   * <p>
   * <strong>Default:</strong> []
   *
   * @return Array of paths to public keys
   */
  public List<Path> publicKeys() {
    return getListOfPath("publickeys");
  }

  /**
   * The corresponding set of private keys. These must correspond to the public keys listed <i>publicKeys</i>.
   *
   * <p>
   * <strong>Default:</strong> []
   *
   * @see #publicKeys()
   * @return Array of paths to corresponding private keys
   */
  public List<Path> privateKeys() {
    return getListOfPath("privatekeys");
  }

  /**
   * Optional list of paths to public keys to add as recipients for every transaction sent through this node, e.g. for
   * backup purposes. These keys must be advertised by some Orion node on the network, i.e. be in a node's
   * <i>publicKeys/privateKeys</i> lists.
   *
   * <p>
   * <strong>Default:</strong> []
   *
   * @see #publicKeys()
   * @see #privateKeys()
   * @return Array of paths to public keys that are always included as recipients
   */
  public List<Path> alwaysSendTo() {
    return getListOfPath("alwayssendto");
  }

  /**
   * Optional file containing the passwords needed to unlock the given <i>privateKeys</i> The file should contain one
   * password per line -- add an empty line if any one key isn't locked.
   *
   * @see #privateKeys()
   * @return A file containing the passwords for the specified privateKeys
   */
  public Optional<Path> passwords() {
    if (!configuration.contains("passwords")) {
      return Optional.empty();
    }
    return Optional.of(configuration.getString("passwords")).map(workDir::resolve);
  }

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
  public String storage() {
    return configuration.getString("storage");
  }

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
  public String tls() {
    return configuration.getString("tls").toLowerCase();
  }

  /**
   * File containing the server's TLS certificate in Apache format. This is used to identify this node to other nodes in
   * the network when they connect to the public API. If it doesn't exist it will be created.
   *
   * <p>
   * <strong>Default:</strong> "tls-server-cert.pem"
   *
   * @return TLS certificate file to use for the public API
   */
  public Path tlsServerCert() {
    return getPath("tlsservercert");
  }

  /**
   * List of files that constitute the CA trust chain for the server certificate. This can be empty for
   * auto-generated/non-PKI-based certificates.
   *
   * <p>
   * <strong>Default:</strong> []
   *
   * @return Array of TLS chain certificates to use for the public API
   */
  public List<Path> tlsServerChain() {
    return getListOfPath("tlsserverchain");
  }

  /**
   * The private key file for the server TLS certificate. If the file doesn't exist it will be created.
   *
   * <p>
   * <strong>Default:</strong> "tls-server-key.pem"
   *
   * @return TLS key to use for the public API
   */
  public Path tlsServerKey() {
    return getPath("tlsserverkey");
  }

  /**
   * TLS trust mode for the server. This decides who's allowed to connect to it. Options:
   *
   * <ul>
   * <li><strong>whitelist:</strong> Only nodes that have previously connected to this node and been added to the
   * <i>tlsKnownClients</i> file will be allowed to connect. This mode will not add any new clients to the
   * <i>tlsKnownClients</i> file.
   *
   * <li><strong>tofu:</strong> (Trust-on-first-use) Only the first node that connects identifying as a certain host
   * will be allowed to connect as the same host in the future. Note that nodes identifying as other hosts will still be
   * able to connect - switch to whitelist after populating the <i>tlsKnownClients</i> list to restrict access.
   *
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
  public String tlsServerTrust() {
    return configuration.getString("tlsservertrust").toLowerCase();
  }

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
  public Path tlsKnownClients() {
    return getPath("tlsknownclients");
  }

  /**
   * File containing the client's TLS certificate in Apache format. This is used to identify this node to other nodes in
   * the network when it is connecting to their public APIs. If it doesn't exist it will be created
   *
   * <p>
   * <strong>Default:</strong> "tls-client-cert.pem"
   *
   * @return TLS client certificate file
   */
  public Path tlsClientCert() {
    String key = "tlsclientcert";
    return getPath(key);
  }

  /**
   * List of files that constitute the CA trust chain for the client certificate. This can be empty for
   * auto-generated/non-PKI-based certificates.
   *
   * <p>
   * <strong>Default:</strong> []
   *
   * @return Array of TLS chain certificates to use for connections to other nodes
   */
  public List<Path> tlsClientChain() {
    return getListOfPath("tlsclientchain");
  }

  /**
   * The private key file for the client TLS certificate. If it doesn't exist it will be created.
   *
   * <p>
   * <strong>Default:</strong> "tls-client-key.pem"
   *
   * @return TLS key to use for connections to other nodes
   */
  public Path tlsClientKey() {
    return getPath("tlsclientkey");
  }

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
  public String tlsClientTrust() {
    return configuration.getString("tlsclienttrust").toLowerCase();
  }

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
  public Path tlsKnownServers() {
    return getPath("tlsknownservers");
  }

  private URL getURL(String key) {
    try {
      return new URL(configuration.getString(key));
    } catch (MalformedURLException e) {
      throw new IllegalStateException("key '" + key + "' should have been validated, yet it's invalid", e);
    }
  }

  private Path getPath(String key) {
    return workDir.resolve(configuration.getString(key));
  }

  private List<Path> getListOfPath(String key) {
    return configuration.getListOfString(key).stream().map(workDir::resolve).collect(Collectors.toList());
  }

  private static Schema configSchema() {
    SchemaBuilder schemaBuilder = SchemaBuilder.create();

    schemaBuilder.addString(
        "nodeurl",
        "http://127.0.0.1:8080/",
        "Externally accessible URL for this node's public API This is what is advertised to other nodes on the network and must be reachable by them.",
        isURL());

    schemaBuilder.addInteger("nodeport", 8080, "Port to listen on for the Orion API.", inRange(1, 65536));

    schemaBuilder
        .addString("nodenetworkinterface", "127.0.0.1", "Network interface to which the Orion API will bind.", null);

    schemaBuilder.addString(
        "clienturl",
        "http://127.0.0.1:8888/",
        "Internally accessible URL for this node's public API This is what is advertised to other nodes on the network and must be reachable by them.",
        isURL());

    schemaBuilder.addInteger("clientport", 8888, "Port to listen on for the Ethereum client API.", inRange(1, 65536));

    schemaBuilder.addString(
        "clientnetworkinterface",
        "127.0.0.1",
        "Network interface to which the Ethereum client API will bind.",
        null);

    schemaBuilder.addString(
        "workdir",
        Paths.get(System.getProperty("user.dir")).toAbsolutePath().toString(),
        "Directory to which paths to all other files referenced in the config are relative to.",
        null);

    schemaBuilder.addListOfString(
        "othernodes",
        Collections.emptyList(),
        "Initial list of other nodes in the network. Orion will automatically connect "
            + "to other nodes not in this list that are advertised by the nodes below, so these can be considered the \"boot nodes.\"",
        allInList(isURL()));

    schemaBuilder
        .addListOfString("publickeys", Collections.emptyList(), "The set of public keys this node will host.", null);

    schemaBuilder.addListOfString(
        "privatekeys",
        Collections.emptyList(),
        "The corresponding set of private keys. These must correspond to the public keys listed 'publickeys'.",
        null);

    schemaBuilder.documentProperty("libsodiumpath", "The path at which to locate the lib sodium shared library");

    schemaBuilder.addListOfString(
        "alwayssendto",
        Collections.emptyList(),
        "Optional comma-separated list of paths to public keys to add as recipients for every transaction sent through this node, e.g. for backup purposes. These keys must be advertised by some Orion node on the network, i.e. be in a node's 'publickeys/privatekeys' lists.",
        null);

    schemaBuilder.documentProperty(
        "passwords",
        "Path to an optional file containing the passwords needed to unlock the given 'privatekeys'. The file should contain one password per line -- add an empty line if any one key isn't locked.");

    schemaBuilder.addString(
        "storage",
        "leveldb",
        "Storage engine used to save payloads and related information. Options:\n"
            + "\n"
            + "   - leveldb:path - LevelDB\n"
            + "   - mapdb:path - MapDB\n"
            + "   - memory - Contents are cleared when Orion exits",
        Config::validateStorage);

    schemaBuilder.addListOfString(
        "ipwhitelist",
        Collections.emptyList(),
        "Optional IP whitelist for the node API. If unspecified/empty, connections from all sources will be allowed (but the private API remains accessible only via the IPC socket.) To allow connections from localhost when a whitelist is defined, e.g. when running multiple Orion nodes on the same machine, add \"127.0.0.1\" and \"::1\" to this list.",
        null);

    schemaBuilder.addString(
        "tls",
        "strict",
        "TLS status. Options:\n"
            + "\n"
            + "   - strict: All connections to and from this node must use TLS with mutual\n"
            + "       authentication. See the documentation for 'tlsservertrust' and 'tlsclienttrust'\n"
            + "   - off: Mutually authenticated TLS is not used for in- and outbound\n"
            + "       connections, although unauthenticated connections to HTTPS hosts are still possible. This\n"
            + "       should only be used if another transport security mechanism like WireGuard is in place.",
        PropertyValidator.anyOfIgnoreCase("off", "strict"));

    schemaBuilder.addString(
        "tlsservercert",
        "tls-server-cert.pem",
        "containing the server's TLS certificate in Apache format. This is used to identify this node to other nodes in the network when they connect to the public API. If it doesn't exist it will be created.",
        null);

    schemaBuilder.addListOfString(
        "tlsserverchain",
        Collections.emptyList(),
        "List of files that constitute the CA trust chain for the server certificate. This can be empty for auto-generated/non-PKI-based certificates.",
        null);

    schemaBuilder.addString(
        "tlsserverkey",
        "tls-server-key.pem",
        "The private key for the server TLS certificate. If the doesn't exist it will be created.",
        null);

    schemaBuilder.addString(
        "tlsservertrust",
        "tofu",
        "TLS trust mode for the server. This decides who's allowed to connect to it. Options:\n"
            + "\n"
            + "   - whitelist: Only nodes presenting certificates with fingerprints in 'tlsknownclients'\n"
            + "       will be allowed to connect.\n"
            + "   - ca: Only nodes with a valid certificate and chain of trust to one of the\n"
            + "       system root certificates will be allowed to connect. The folder containing trusted root\n"
            + "       certificates can be overridden with the SYSTEM_CERTIFICATE_PATH environment variable.\n"
            + "   - insecure-tofa: (Trust-on-first-access) On first connection to this server the common name\n"
            + "       and fingerprint of the presented certificate will be added to 'tlsknownclients'. On\n"
            + "       subsequent connections, the client will be rejected if the fingerprint has changed.\n"
            + "   - insecure-ca-or-tofa: A combination of ca and tofa: If the client presents a certificate\n"
            + "       signed by a trusted CA, it will be accepted. If it is self-signed, it\n"
            + "       will be allowed only if it's the first certificate this node has seen for that host.\n"
            + "   - insecure-record: Any client can connect and the fingerprint of their certificate will be\n"
            + "       added to the 'tlsknownclients' file.\n",
        PropertyValidator.anyOfIgnoreCase(
            "whitelist",
            "ca",
            "ca-or-whitelist",
            "tofu",
            "insecure-tofa",
            "ca-or-tofu",
            "insecure-ca-or-tofa",
            "insecure-no-validation",
            "insecure-record",
            "insecure-ca-or-record"));

    schemaBuilder.addString(
        "tlsknownclients",
        "tls-known-clients",
        "TLS known clients for the server. This contains the fingerprints of public keys of other nodes that are allowed to connect to this one for the ca-or-tofu, tofu and whitelist trust modes",
        null);

    schemaBuilder.addString(
        "tlsclientcert",
        "tls-client-cert.pem",
        "containing the client's TLS certificate in Apache format. This is used to identify this node to other nodes in the network when it is connecting to their public APIs. If it doesn't exist it will be created",
        null);

    schemaBuilder.addListOfString(
        "tlsclientchain",
        Collections.emptyList(),
        "List of files that constitute the CA trust chain for the client certificate. This can be empty for auto-generated/non-PKI-based certificates.",
        null);

    schemaBuilder.addString(
        "tlsclientkey",
        "tls-client-key.pem",
        "The private key for the client TLS certificate. If it doesn't exist it will be created.",
        null);

    schemaBuilder.addString(
        "tlsclienttrust",
        "ca-or-tofu",
        "TLS trust mode for the client. This decides which servers it will connect to. Options:\n"
            + "\n"
            + "   - whitelist: This node will only connect to servers it has previously seen\n"
            + "       and added to the 'tlsknownservers' file. This mode will not add any new servers to\n"
            + "       the 'tlsknownservers' file.\n"
            + "   - ca: The node will only connect to servers with a valid certificate and\n"
            + "       chain of trust to one of the system root certificates. The folder containing trusted root\n"
            + "       certificates can be overridden with the SYSTEM_CERTIFICATE_PATH environment variable.\n"
            + "   - tofu: (Trust-on-first-use) This node will only connect to the same\n"
            + "       server for any given host. (Similar to how OpenSSH works.)\n"
            + "   - ca-or-tofu: A combination of ca and tofu: If a certificate is valid, it\n"
            + "       is always allowed and added to the 'tlsknownservers' list. If it is self-signed, it\n"
            + "       will be allowed only if it's the first certificate this node has seen for that host.\n"
            + "   - insecure-record: This node will connect to any server, regardless\n"
            + "       of certificate, however it will still be added to the 'tlsknownservers' file.",
        PropertyValidator.anyOfIgnoreCase(
            "whitelist",
            "ca",
            "ca-or-whitelist",
            "tofu",
            "ca-or-tofu",
            "insecure-no-validation",
            "insecure-record",
            "insecure-ca-or-record"));

    schemaBuilder.addString(
        "tlsknownservers",
        "tls-known-servers",
        "TLS known servers for the client. This contains the fingerprints of public keys of other nodes that this node has encountered for the ca-or-tofu, tofu and whitelist trust modes.",
        null);

    schemaBuilder.addInteger("verbosity", 1, "Verbosity level (each level includes all prior levels)", inRange(0, 4));

    schemaBuilder.validateConfiguration(Config::validateConfiguration);

    return schemaBuilder.toSchema();
  }

  private static List<ConfigurationError> validateStorage(
      String key,
      @Nullable DocumentPosition position,
      @Nullable String value) {
    assert (value != null);
    String storageType = value.split(":", 2)[0];
    if (!Arrays.asList("mapdb", "leveldb", "memory").contains(storageType)) {
      return singleError(
          position,
          "Value of property '" + key + "' must have storage type of \"leveldb\", \"mapdb\", or \"memory\"");
    }
    return noErrors();
  }

  private static List<ConfigurationError> validateConfiguration(Configuration config) {
    List<ConfigurationError> errors = new ArrayList<>();

    if (config.contains("nodeport")
        && config.contains("clientport")
        && config.getInteger("nodeport") == config.getInteger("clientport")) {
      errors.add(
          new ConfigurationError(
              config.inputPositionOf("clientport"),
              "Value of property 'nodeport' must be different to 'clientport'"));
    }

    if (config.contains("publickeys")
        && config.contains("privatekeys")
        && config.getListOfString("publickeys").size() != config.getListOfString("privatekeys").size()) {
      errors.add(
          new ConfigurationError(
              config.inputPositionOf("privatekeys"),
              "The number of keys specified for properties 'publickeys' and 'privatekeys' must be the same"));
    }

    return errors;
  }
}
