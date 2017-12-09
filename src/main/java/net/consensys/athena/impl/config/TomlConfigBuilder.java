package net.consensys.athena.impl.config;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.api.config.ConfigException;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import com.moandjiezana.toml.Toml;

public class TomlConfigBuilder {

  Config build(InputStream config) throws ConfigException {

    boolean configIssues = false;
    String errorMsg = "Invalid Configuration Options\n";
    MemoryConfig memoryConfig = new MemoryConfig();

    Toml toml = new Toml().read(config);

    if (toml.getString("url") != null) memoryConfig.setUrl(toml.getString("url"));
    else {
      configIssues = true;
      errorMsg += "URL must be specified.\n";
    }

    if (toml.getLong("port") != null) memoryConfig.setPort(toml.getLong("port"));
    else {
      configIssues = true;
      errorMsg += "Port must be specified.\n";
    }

    if (toml.getString("workdir") != null)
      memoryConfig.setWorkDir(new File(toml.getString("workdir")));

    if (toml.getString("socket") != null)
      memoryConfig.setSocket(new File(toml.getString("socket")));

    memoryConfig.setOtherNodes(convertListToFileArray(toml.getList("othernodes")));
    memoryConfig.setPublicKeys(convertListToFileArray(toml.getList("publickeys")));
    memoryConfig.setPrivateKeys(convertListToFileArray(toml.getList("privatekeys")));
    memoryConfig.setAlwaysSendTo(convertListToFileArray(toml.getList("alwayssendto")));

    if (toml.getString("passwords") != null)
      memoryConfig.setPasswords(new File(toml.getString("passwords")));

    memoryConfig.setStorage(toml.getString("storage", "dir:storage"));
    if (!validateStorageTypes(memoryConfig.storage())) {
      configIssues = true;
      errorMsg += "Invalid Storage type.\n";
    }

    if (!validateStoragePathsExist(memoryConfig.storage())) {
      configIssues = true;
      errorMsg += "Path must exist for Storage type.\n";
    }

    memoryConfig.setIpWhitelist(convertListToStringArray(toml.getList("ipwhitelist")));

    memoryConfig.setTls(toml.getString("tls", "strict"));
    if (!validateTLS(memoryConfig.tls())) {
      configIssues = true;
      errorMsg += "Invalid TLS Status.\n";
    }

    memoryConfig.setTlsServerCert(new File(toml.getString("tlsservercert", "tls-server-cert.pem")));
    memoryConfig.setTlsServerChain(convertListToFileArray(toml.getList("tlsserverchain")));
    memoryConfig.setTlsServerKey(new File(toml.getString("tlsserverkey", "tls-server-key.pem")));

    memoryConfig.setTlsServerTrust(toml.getString("tlsservertrust", "tofu"));
    if (!validateTrustMode(memoryConfig.tlsServerTrust())) {
      configIssues = true;
      errorMsg += "Invalid TLS Server Trust mode.\n";
    }

    memoryConfig.setTlsKnownClients(
        new File(toml.getString("tlsknownclients", "tls-known-clients")));
    memoryConfig.setTlsClientCert(new File(toml.getString("tlsclientcert", "tls-client-cert.pem")));
    memoryConfig.setTlsClientChain(convertListToFileArray(toml.getList("tlsclientchain")));
    memoryConfig.setTlsClientKey(new File(toml.getString("tlsclientkey", "tls-client-key.pem")));

    memoryConfig.setTlsClientTrust(toml.getString("tlsclienttrust", "ca-or-tofu"));
    if (!validateTrustMode(memoryConfig.tlsClientTrust())) {
      configIssues = true;
      errorMsg += "Invalid TLS Client Trust mode.\n";
    }

    memoryConfig.setTlsKnownServers(
        new File(toml.getString("tlsknownservers", "tls-known-servers")));

    memoryConfig.setVerbosity(toml.getLong("verbosity", (long) 1));
    if (!validateVerbosity(memoryConfig.verbosity())) {
      configIssues = true;
      errorMsg += "Invalid verbosity.\n";
    }

    if (configIssues) throw new ConfigException(errorMsg);

    return memoryConfig;
  }

  private File[] convertListToFileArray(List<String> paths) {
    return paths == null ? new File[0] : paths.stream().map(File::new).toArray(File[]::new);
  }

  private String[] convertListToStringArray(List<String> paths) {
    return paths == null ? new String[0] : paths.toArray(new String[paths.size()]);
  }

  boolean validateTrustMode(String mode) {
    List<String> validModes =
        Arrays.asList("whitelist", "tofu", "ca", "ca-or-tofu", "insecure-no-validation");
    return validModes.stream().anyMatch(mode::equals);
  }

  boolean validateVerbosity(long verbosity) {
    return verbosity >= 0 && verbosity <= 3;
  }

  boolean validateStorageTypes(String storage) {

    return storage.startsWith("bdp:")
        || storage.startsWith("dir:")
        || storage.startsWith("leveldb:")
        || storage.equals("memory")
        || storage.startsWith("sqlite:");
  }

  boolean validateStoragePathsExist(String storage) {

    String[] storageConfig = storage.split(":");

    return !storage.contains(":") || storageConfig.length == 2;
  }

  boolean validateTLS(String tls) {
    return tls.equals("strict") || tls.equals("off");
  }
}
