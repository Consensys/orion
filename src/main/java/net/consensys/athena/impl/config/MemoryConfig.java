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

    public MemoryConfig(String url, long port, File workDir, File socket, File[] otherNodes, File[] publicKeys,
                        File[] privateKeys, File[] alwaysSendTo, File passwords, String storage, String[] ipWhitelist,
                        String tls, File tlsServerCert, File[] tlsServerChain, File tlsServerKey, String tlsServerTrust,
                        File tlsKnownClients, File tlsClientCert, File[] tlsClientChain, File tlsClientKey,
                        String tlsClientTrust, File tlsKnownServers, String[] justGenerateKeys,
                        boolean justShowVersion, long verbosity) {
        this.url = url;
        this.port = port;
        this.workDir = workDir;
        this.socket = socket;
        this.otherNodes = otherNodes;
        this.publicKeys = publicKeys;
        this.privateKeys = privateKeys;
        this.alwaysSendTo = alwaysSendTo;
        this.passwords = passwords;
        this.storage = storage;
        this.ipWhitelist = ipWhitelist;
        this.tls = tls;
        this.tlsServerCert = tlsServerCert;
        this.tlsServerChain = tlsServerChain;
        this.tlsServerKey = tlsServerKey;
        this.tlsServerTrust = tlsServerTrust;
        this.tlsKnownClients = tlsKnownClients;
        this.tlsClientCert = tlsClientCert;
        this.tlsClientChain = tlsClientChain;
        this.tlsClientKey = tlsClientKey;
        this.tlsClientTrust = tlsClientTrust;
        this.tlsKnownServers = tlsKnownServers;
        this.justGenerateKeys = justGenerateKeys;
        this.justShowVersion = justShowVersion;
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
