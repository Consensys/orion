package net.consensys.athena.impl.config;

import net.consensys.athena.api.config.Config;

import java.io.File;
import java.util.Optional;

public class MemoryConfig implements Config{

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
    public String getUrl() {
        return url;
    }

    @Override
    public long getPort() {
        return port;
    }

    @Override
    public Optional<File> getWorkDir() {
        return workDir;
    }

    @Override
    public Optional<File> getSocket() {
        return socket;
    }

    @Override
    public File[] getOtherNodes() {
        return otherNodes;
    }

    @Override
    public File[] getPublicKeys() {
        return publicKeys;
    }

    @Override
    public File[] getPrivateKeys() {
        return privateKeys;
    }

    @Override
    public File[] getAlwaysSendTo() {
        return alwaysSendTo;
    }

    @Override
    public Optional<File> getPasswords() {
        return passwords;
    }

    @Override
    public String getStorage() {
        return storage;
    }

    @Override
    public String[] getIpWhitelist() {
        return ipWhitelist;
    }

    @Override
    public String getTls() {
        return tls;
    }

    @Override
    public File getTlsServerCert() {
        return tlsServerCert;
    }

    @Override
    public File[] getTlsServerChain() {
        return tlsServerChain;
    }

    @Override
    public File getTlsServerKey() {
        return tlsServerKey;
    }

    @Override
    public String getTlsServerTrust() {
        return tlsServerTrust;
    }

    @Override
    public File getTlsKnownClients() {
        return tlsKnownClients;
    }

    @Override
    public File getTlsClientCert() {
        return tlsClientCert;
    }

    @Override
    public File[] getTlsClientChain() {
        return tlsClientChain;
    }

    @Override
    public File getTlsClientKey() {
        return tlsClientKey;
    }

    @Override
    public String getTlsClientTrust() {
        return tlsClientTrust;
    }

    @Override
    public File getTlsKnownServers() {
        return tlsKnownServers;
    }

    @Override
    public String[] getJustGenerateKeys() {
        return justGenerateKeys;
    }

    @Override
    public boolean getJustShowVersion() {
        return justShowVersion;
    }

    @Override
    public long getVerbosity() {
        return verbosity;
    }
}
