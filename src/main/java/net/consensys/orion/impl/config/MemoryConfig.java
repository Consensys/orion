package net.consensys.orion.impl.config;

import net.consensys.orion.api.config.Config;
import net.consensys.orion.impl.enclave.sodium.LibSodiumSettings;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class MemoryConfig implements Config {

  private URL nodeUrl;
  private int nodePort = 8080;
  private String nodeNetworkInterface = "127.0.0.1";
  private URL clientUrl;
  private int clientPort = 8888;
  private String clientNetworkInterface = "127.0.0.1";
  private Path workDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
  private URL[] otherNodes = new URL[] {};
  private Path[] publicKeys = new Path[] {};
  private Path[] privateKeys = new Path[] {};
  private Path[] alwaysSendTo = new Path[] {};
  private Optional<Path> passwords = Optional.empty();
  private String storage = "leveldb";
  private String tls = "strict";
  private Path tlsServerCert = Paths.get("tls-server-cert.pem");
  private Path[] tlsServerChain = new Path[] {};
  private Path tlsServerKey = Paths.get("tls-server-key.pem");
  private String tlsServerTrust = "tofu";
  private Path tlsKnownClients = Paths.get("tls-known-clients");
  private Path tlsClientCert = Paths.get("tls-client-cert.pem");
  private Path[] tlsClientChain = new Path[] {};
  private Path tlsClientKey = Paths.get("tls-client-key.pem");
  private String tlsClientTrust = "ca-or-tofu";
  private Path tlsKnownServers = Paths.get("tls-known-servers");
  private Optional<String[]> generateKeys = Optional.empty();
  private Optional<Boolean> showVersion = Optional.empty();
  private String libSodiumPath = LibSodiumSettings.defaultLibSodiumPath();

  public MemoryConfig() {
    try {
      nodeUrl = new URL("https://localhost:8080");
      clientUrl = new URL("https://localhost:8888");
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }


  public void setNodeUrl(URL nodeUrl) {
    this.nodeUrl = nodeUrl;
  }

  public void setNodePort(int nodePort) {
    this.nodePort = nodePort;
  }

  public void setNodeNetworkInterface(String nodeNetworkInterface) {
    this.nodeNetworkInterface = nodeNetworkInterface;
  };

  public void setClientUrl(URL url) {
    this.clientUrl = url;
  }

  public void setClientPort(int port) {
    this.clientPort = port;
  }

  public void setClientNetworkInterface(String clientNetworkInterface) {
    this.clientNetworkInterface = clientNetworkInterface;
  }

  public void setWorkDir(Path workDir) {
    this.workDir = workDir;
  }

  public void setOtherNodes(URL[] otherNodes) {
    this.otherNodes = otherNodes;
  }

  public void setPublicKeys(Path... publicKeys) {
    this.publicKeys = publicKeys;
  }

  public void setPrivateKeys(Path... privateKeys) {
    this.privateKeys = privateKeys;
  }

  public void setAlwaysSendTo(Path... alwaysSendTo) {
    this.alwaysSendTo = alwaysSendTo;
  }

  public void setPasswords(Path passwords) {
    this.passwords = Optional.ofNullable(passwords);
  }

  public void setStorage(String storage) {
    this.storage = storage;
  }

  public void setTls(String tls) {
    this.tls = tls;
  }

  public void setTlsServerCert(Path tlsServerCert) {
    this.tlsServerCert = tlsServerCert;
  }

  public void setTlsServerChain(Path[] tlsServerChain) {
    this.tlsServerChain = tlsServerChain;
  }

  public void setTlsServerKey(Path tlsServerKey) {
    this.tlsServerKey = tlsServerKey;
  }

  public void setTlsServerTrust(String tlsServerTrust) {
    this.tlsServerTrust = tlsServerTrust;
  }

  public void setTlsKnownClients(Path tlsKnownClients) {
    this.tlsKnownClients = tlsKnownClients;
  }

  public void setTlsClientCert(Path tlsClientCert) {
    this.tlsClientCert = tlsClientCert;
  }

  public void setTlsClientChain(Path[] tlsClientChain) {
    this.tlsClientChain = tlsClientChain;
  }

  public void setTlsClientKey(Path tlsClientKey) {
    this.tlsClientKey = tlsClientKey;
  }

  public void setTlsClientTrust(String tlsClientTrust) {
    this.tlsClientTrust = tlsClientTrust;
  }

  public void setTlsKnownServers(Path tlsKnownServers) {
    this.tlsKnownServers = tlsKnownServers;
  }

  public void setGenerateKeys(String[] generateKeys) {
    this.generateKeys = Optional.ofNullable(generateKeys);
  }

  public void setShowVersion(boolean showVersion) {
    this.showVersion = Optional.ofNullable(showVersion);
  }

  public void setLibSodiumPath(String libSodiumPath) {
    this.libSodiumPath = libSodiumPath;
  }

  @Override
  public URL nodeUrl() {
    return nodeUrl;
  }

  @Override
  public int nodePort() {
    return nodePort;
  }

  @Override
  public String nodeNetworkInterface() {
    return nodeNetworkInterface;
  }

  @Override
  public URL clientUrl() {
    return clientUrl;
  }

  @Override
  public int clientPort() {
    return clientPort;
  }

  @Override
  public String clientNetworkInterface() {
    return clientNetworkInterface;
  }

  @Override
  public Path workDir() {
    return workDir;
  }

  @Override
  public URL[] otherNodes() {
    return otherNodes;
  }

  @Override
  public Path[] publicKeys() {
    return publicKeys;
  }

  @Override
  public Path[] privateKeys() {
    return privateKeys;
  }

  @Override
  public Path[] alwaysSendTo() {
    return alwaysSendTo;
  }

  @Override
  public Optional<Path> passwords() {
    return passwords;
  }

  @Override
  public String storage() {
    return storage;
  }

  @Override
  public String tls() {
    return tls;
  }

  @Override
  public Path tlsServerCert() {
    return tlsServerCert;
  }

  @Override
  public Path[] tlsServerChain() {
    return tlsServerChain;
  }

  @Override
  public Path tlsServerKey() {
    return tlsServerKey;
  }

  @Override
  public String tlsServerTrust() {
    return tlsServerTrust;
  }

  @Override
  public Path tlsKnownClients() {
    return tlsKnownClients;
  }

  @Override
  public Path tlsClientCert() {
    return tlsClientCert;
  }

  @Override
  public Path[] tlsClientChain() {
    return tlsClientChain;
  }

  @Override
  public Path tlsClientKey() {
    return tlsClientKey;
  }

  @Override
  public String tlsClientTrust() {
    return tlsClientTrust;
  }

  @Override
  public Path tlsKnownServers() {
    return tlsKnownServers;
  }

  @Override
  public Optional<String[]> generateKeys() {
    return generateKeys;
  }

  @Override
  public Optional<Boolean> showVersion() {
    return showVersion;
  }

  @Override
  public String libSodiumPath() {
    return libSodiumPath;
  }
}
