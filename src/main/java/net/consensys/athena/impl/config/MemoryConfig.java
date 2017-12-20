package net.consensys.athena.impl.config;

import net.consensys.athena.api.config.Config;

import java.io.File;
import java.net.URL;
import java.util.Optional;

public class MemoryConfig implements Config {

  private URL url;
  private long port;
  private Optional<File> workDir = Optional.empty();
  private Optional<File> socket = Optional.empty();
  private URL[] otherNodes = new URL[] {};
  private File[] publicKeys = new File[] {};
  private File[] privateKeys = new File[] {};
  private File[] alwaysSendTo = new File[] {};
  private Optional<File> passwords = Optional.empty();
  private String storage;
  private String[] ipWhitelist = new String[] {};
  private String tls;
  private File tlsServerCert;
  private File[] tlsServerChain = new File[] {};
  private File tlsServerKey;
  private String tlsServerTrust;
  private File tlsKnownClients;
  private File tlsClientCert;
  private File[] tlsClientChain = new File[] {};
  private File tlsClientKey;
  private String tlsClientTrust;
  private File tlsKnownServers;
  private Optional<String[]> generateKeys = Optional.empty();
  private Optional<Boolean> showVersion = Optional.empty();
  private long verbosity;
  private String libSodiumPath;

  public void setUrl(URL url) {
    this.url = url;
  }

  public void setPort(long port) {
    this.port = port;
  }

  public void setWorkDir(File workDir) {
    this.workDir = Optional.ofNullable(workDir);
  }

  public void setSocket(File socket) {
    this.socket = Optional.ofNullable(socket);
  }

  public void setOtherNodes(URL[] otherNodes) {
    this.otherNodes = otherNodes;
  }

  public void setPublicKeys(File[] publicKeys) {
    this.publicKeys = publicKeys;
  }

  public void setPrivateKeys(File[] privateKeys) {
    this.privateKeys = privateKeys;
  }

  public void setAlwaysSendTo(File[] alwaysSendTo) {
    this.alwaysSendTo = alwaysSendTo;
  }

  public void setPasswords(File passwords) {
    this.passwords = Optional.ofNullable(passwords);
  }

  public void setStorage(String storage) {
    this.storage = storage;
  }

  public void setIpWhitelist(String[] ipWhitelist) {
    this.ipWhitelist = ipWhitelist;
  }

  public void setTls(String tls) {
    this.tls = tls;
  }

  public void setTlsServerCert(File tlsServerCert) {
    this.tlsServerCert = tlsServerCert;
  }

  public void setTlsServerChain(File[] tlsServerChain) {
    this.tlsServerChain = tlsServerChain;
  }

  public void setTlsServerKey(File tlsServerKey) {
    this.tlsServerKey = tlsServerKey;
  }

  public void setTlsServerTrust(String tlsServerTrust) {
    this.tlsServerTrust = tlsServerTrust;
  }

  public void setTlsKnownClients(File tlsKnownClients) {
    this.tlsKnownClients = tlsKnownClients;
  }

  public void setTlsClientCert(File tlsClientCert) {
    this.tlsClientCert = tlsClientCert;
  }

  public void setTlsClientChain(File[] tlsClientChain) {
    this.tlsClientChain = tlsClientChain;
  }

  public void setTlsClientKey(File tlsClientKey) {
    this.tlsClientKey = tlsClientKey;
  }

  public void setTlsClientTrust(String tlsClientTrust) {
    this.tlsClientTrust = tlsClientTrust;
  }

  public void setTlsKnownServers(File tlsKnownServers) {
    this.tlsKnownServers = tlsKnownServers;
  }

  public void setGenerateKeys(String[] generateKeys) {
    this.generateKeys = Optional.ofNullable(generateKeys);
  }

  public void setShowVersion(boolean showVersion) {
    this.showVersion = Optional.ofNullable(showVersion);
  }

  public void setVerbosity(long verbosity) {
    this.verbosity = verbosity;
  }

  public void setLibSodiumPath(String libSodiumPath) {
    this.libSodiumPath = libSodiumPath;
  }

  @Override
  public URL url() {
    return url;
  }

  @Override
  public long port() {
    return port;
  }

  @Override
  public Optional<File> workDir() {
    return workDir;
  }

  @Override
  public Optional<File> socket() {
    return socket;
  }

  @Override
  public URL[] otherNodes() {
    return otherNodes;
  }

  @Override
  public File[] publicKeys() {
    return publicKeys;
  }

  @Override
  public File[] privateKeys() {
    return privateKeys;
  }

  @Override
  public File[] alwaysSendTo() {
    return alwaysSendTo;
  }

  @Override
  public Optional<File> passwords() {
    return passwords;
  }

  @Override
  public String storage() {
    return storage;
  }

  @Override
  public String[] ipWhitelist() {
    return ipWhitelist;
  }

  @Override
  public String tls() {
    return tls;
  }

  @Override
  public File tlsServerCert() {
    return tlsServerCert;
  }

  @Override
  public File[] tlsServerChain() {
    return tlsServerChain;
  }

  @Override
  public File tlsServerKey() {
    return tlsServerKey;
  }

  @Override
  public String tlsServerTrust() {
    return tlsServerTrust;
  }

  @Override
  public File tlsKnownClients() {
    return tlsKnownClients;
  }

  @Override
  public File tlsClientCert() {
    return tlsClientCert;
  }

  @Override
  public File[] tlsClientChain() {
    return tlsClientChain;
  }

  @Override
  public File tlsClientKey() {
    return tlsClientKey;
  }

  @Override
  public String tlsClientTrust() {
    return tlsClientTrust;
  }

  @Override
  public File tlsKnownServers() {
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
  public long verbosity() {
    return verbosity;
  }

  @Override
  public String libSodiumPath() {
    return libSodiumPath;
  }
}
