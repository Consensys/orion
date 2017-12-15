package net.consensys.athena.impl.config;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.api.config.ConfigException;
import net.consensys.athena.impl.enclave.sodium.LibSodiumSettings;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import com.moandjiezana.toml.Toml;

public class TomlConfigBuilder {

  public Config build(InputStream config) throws ConfigException {
    StringBuilder errorMsg = new StringBuilder();
    MemoryConfig memoryConfig = new MemoryConfig();

    Toml toml = new Toml().read(config);

    if (toml.getString("url") != null) {
      try {
        memoryConfig.setUrl(new URL(toml.getString("url")));
      } catch (MalformedURLException e) {
        errorMsg.append("Error: key 'url' in config is malformed.\n\t");
        errorMsg.append(e.getMessage()).append("\n");
      }
    } else {
      errorMsg.append("Error: value for key 'url' in config must be specified\n");
    }

    if (toml.getLong("port") != null) {
      memoryConfig.setPort(toml.getLong("port"));
    } else {
      errorMsg.append("Error: value for key 'port' in config must be specified\n");
    }

    if (toml.getString("workdir") != null) {
      memoryConfig.setWorkDir(new File(toml.getString("workdir")));
    }

    if (toml.getString("socket") != null) {
      memoryConfig.setSocket(new File(toml.getString("socket")));
    }

    memoryConfig.setLibSodiumPath(
        toml.getString("libsodiumpath", LibSodiumSettings.defaultLibSodiumPath()));

    try {
      memoryConfig.setOtherNodes(convertListToURLArray(toml.getList("othernodes")));
    } catch (ConfigException e) {
      errorMsg.append("Error: key 'othernodes' in config containes malformed URLS.\n");
      errorMsg.append(e.getMessage());
    }

    memoryConfig.setPublicKeys(convertListToFileArray(toml.getList("publickeys")));
    memoryConfig.setPrivateKeys(convertListToFileArray(toml.getList("privatekeys")));
    if (memoryConfig.publicKeys().length != memoryConfig.privateKeys().length) {
      errorMsg.append(
          "Error: the number of keys specified for keys 'publickeys' and 'privatekeys' must be the same\n");
    }

    memoryConfig.setAlwaysSendTo(convertListToFileArray(toml.getList("alwayssendto")));

    if (toml.getString("passwords") != null) {
      memoryConfig.setPasswords(new File(toml.getString("passwords")));
    }

    memoryConfig.setStorage(toml.getString("storage", "dir:storage"));
    if (!validateStorageTypes(memoryConfig.storage())) {
      errorMsg.append(
          "Error: value for key 'storage' type must start with: ['bdp:', 'dir:', 'leveldb:', 'sqllite:'] or be 'memory'\n");
    }

    if (!validateStoragePathsExist(memoryConfig.storage())) {
      errorMsg.append(
          "Error: value for key 'storage' of types ['bdp:', 'dir:', 'leveldb:', 'sqllite:'] must specify a path\n");
    }

    memoryConfig.setIpWhitelist(convertListToStringArray(toml.getList("ipwhitelist")));

    memoryConfig.setTls(toml.getString("tls", "strict"));
    if (!validateTLS(memoryConfig.tls())) {
      errorMsg.append("Error: value for key 'tls' status must be 'strict' or 'off'\n");
    }

    memoryConfig.setTlsServerCert(new File(toml.getString("tlsservercert", "tls-server-cert.pem")));
    memoryConfig.setTlsServerChain(convertListToFileArray(toml.getList("tlsserverchain")));
    memoryConfig.setTlsServerKey(new File(toml.getString("tlsserverkey", "tls-server-key.pem")));

    memoryConfig.setTlsServerTrust(toml.getString("tlsservertrust", "tofu"));
    if (!validateTrustMode(memoryConfig.tlsServerTrust())) {
      errorMsg.append(
          "Error: value for key 'tlsservertrust' mode must must be one of ['whitelist', 'tofu', 'ca', 'ca-or-tofu', 'insecure-no-validation']\n");
    }

    memoryConfig.setTlsKnownClients(
        new File(toml.getString("tlsknownclients", "tls-known-clients")));
    memoryConfig.setTlsClientCert(new File(toml.getString("tlsclientcert", "tls-client-cert.pem")));
    memoryConfig.setTlsClientChain(convertListToFileArray(toml.getList("tlsclientchain")));
    memoryConfig.setTlsClientKey(new File(toml.getString("tlsclientkey", "tls-client-key.pem")));

    memoryConfig.setTlsClientTrust(toml.getString("tlsclienttrust", "ca-or-tofu"));
    if (!validateTrustMode(memoryConfig.tlsClientTrust())) {
      errorMsg.append(
          "Error: value for key 'tlsclienttrust' mode must must be one of ['whitelist', 'tofu', 'ca', 'ca-or-tofu', 'insecure-no-validation']\n");
    }

    memoryConfig.setTlsKnownServers(
        new File(toml.getString("tlsknownservers", "tls-known-servers")));

    memoryConfig.setVerbosity(toml.getLong("verbosity", (long) 1));
    if (!validateVerbosity(memoryConfig.verbosity())) {
      errorMsg.append("Error: value for key 'verbosity' must be within range 0 to 3\n");
    }

    if (errorMsg.length() != 0) {
      errorMsg.insert(0, "Invalid Configuration Options\n");
      throw new ConfigException(errorMsg.toString());
    }

    return memoryConfig;
  }

  private File[] convertListToFileArray(List<String> paths) {
    return paths == null ? new File[0] : paths.stream().map(File::new).toArray(File[]::new);
  }

  private URL[] convertListToURLArray(List<String> urls) {
    URL[] urlArray;
    StringBuilder errorMsg = new StringBuilder();

    if (urls == null) {
      urlArray = new URL[0];
    } else {
      urlArray = new URL[urls.size()];
      for (int i = 0; i < urls.size(); i++) {
        try {
          urlArray[i] = new URL(urls.get(i));
        } catch (MalformedURLException e) {
          errorMsg
              .append("\tURL [")
              .append(urls.get(i))
              .append("] ")
              .append(e.getMessage())
              .append("\n");
        }
      }
    }
    if (errorMsg.length() != 0) {
      throw new ConfigException(errorMsg.toString());
    }

    return urlArray;
  }

  private String[] convertListToStringArray(List<String> paths) {
    return paths == null ? new String[0] : paths.toArray(new String[paths.size()]);
  }

  // If options change, error message must also be changed
  boolean validateTrustMode(String mode) {
    List<String> validModes =
        Arrays.asList("whitelist", "tofu", "ca", "ca-or-tofu", "insecure-no-validation");
    return validModes.stream().anyMatch(mode::equals);
  }

  // If options change, error message must also be changed
  boolean validateVerbosity(long verbosity) {
    return verbosity >= 0 && verbosity <= 3;
  }

  // If options change, error message must also be changed
  boolean validateStorageTypes(String storage) {
    return storage.startsWith("bdp:")
        || storage.startsWith("dir:")
        || storage.startsWith("leveldb:")
        || storage.equals("memory")
        || storage.startsWith("sqlite:");
  }

  // If options change, error message must also be changed
  boolean validateStoragePathsExist(String storage) {
    String[] storageConfig = storage.split(":");
    return !storage.contains(":") || storageConfig.length == 2;
  }

  // If options change, error message must also be changed
  boolean validateTLS(String tls) {
    return tls.equals("strict") || tls.equals("off");
  }
}
