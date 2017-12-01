package net.consensys.athena.impl.config;

import net.consensys.athena.api.config.Config;

import java.io.File;
import java.util.Optional;

public class MemoryConfig implements Config {

  private String url;
  private long port;
  private File workDir;
  private File socket;
  private File[] otherNodes;
  private File[] publicKeys;
  private File[] privateKeys;
  private File[] alwaysSendTo;
  private File passwords;
  private String storage;
  private String[] ipWhitelist;
  private String tls;
  private File tlsServerCert;
  private File[] tlsServerChain;
  private File tlsServerKey;
  private String tlsServerTrust;
  private File tlsKnownClients;
  private File tlsClientCert;
  private File[] tlsClientChain;
  private File tlsClientKey;
  private String tlsClientTrust;
  private File tlsKnownServers;
  private String[] justGenerateKeys;
  private boolean justShowVersion;
  private long verbosity;

  public void setUrl(String url) {
    this.url = url;
  }

  public void setPort(long port) {
    this.port = port;
  }

  public void setWorkDir(File workDir) {
    this.workDir = workDir;
  }

  public void setSocket(File socket) {
    this.socket = socket;
  }

  public void setOtherNodes(File[] otherNodes) {
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
    this.passwords = passwords;
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

  public void setJustGenerateKeys(String[] justGenerateKeys) {
    this.justGenerateKeys = justGenerateKeys;
  }

  public void setJustShowVersion(boolean justShowVersion) {
    this.justShowVersion = justShowVersion;
  }

  public void setVerbosity(long verbosity) {
    this.verbosity = verbosity;
  }

  @Override
  public String url() {
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
  public File[] otherNodes() {
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
  public String[] justGenerateKeys() {
    return justGenerateKeys;
  }

  @Override
  public boolean justShowVersion() {
    return justShowVersion;
  }

  @Override
  public long verbosity() {
    return verbosity;
  }
}
