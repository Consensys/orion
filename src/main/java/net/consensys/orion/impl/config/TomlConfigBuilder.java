package net.consensys.orion.impl.config;

import static java.lang.Math.toIntExact;

import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.config.ConfigException;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.moandjiezana.toml.Toml;

public final class TomlConfigBuilder {

  public Config build(InputStream config) {
    StringBuilder errorMsg = new StringBuilder();
    MemoryConfig memoryConfig = new MemoryConfig();

    // read config
    Toml toml = new Toml().read(config);

    if (toml.getString("nodeurl") != null) {
      try {
        memoryConfig.setNodeUrl(new URL(toml.getString("nodeurl")));
      } catch (MalformedURLException e) {
        errorMsg.append("Error: key 'nodeurl' in config is malformed.\n\t");
        errorMsg.append(e.getMessage()).append("\n");
      }
    } else {
      errorMsg.append("Error: value for key 'nodeurl' in config must be specified\n");
    }

    if (toml.getString("clienturl") != null) {
      try {
        memoryConfig.setClientUrl(new URL(toml.getString("clienturl")));
      } catch (MalformedURLException e) {
        errorMsg.append("Error: key 'clienturl' in config is malformed.\n\t");
        errorMsg.append(e.getMessage()).append("\n");
      }
    } else {
      errorMsg.append("Error: value for key 'clienturl' in config must be specified\n");
    }

    // reading and setting workDir first;
    String workDir = toml.getString("workdir");
    Path baseDir;
    if (workDir != null) {
      baseDir = Paths.get(workDir);
      memoryConfig.setWorkDir(baseDir);
    } else {
      baseDir = memoryConfig.workDir();
    }

    setInt(toml.getLong("nodeport"), memoryConfig::setNodePort);
    setString(toml.getString("nodenetworkinterface"), memoryConfig::setNodeNetworkInterface);
    setInt(toml.getLong("clientport"), memoryConfig::setClientPort);
    setString(toml.getString("clientnetworkinterface"), memoryConfig::setClientNetworkInterface);
    setString(toml.getString("libsodiumpath"), memoryConfig::setLibSodiumPath);

    StringBuilder othernodesError = setURLArray(toml.getList("othernodes"), memoryConfig::setOtherNodes);

    setPathArray(baseDir, toml.getList("publickeys"), memoryConfig::setPublicKeys);
    setPathArray(baseDir, toml.getList("privatekeys"), memoryConfig::setPrivateKeys);
    setPathArray(baseDir, toml.getList("alwayssendto"), memoryConfig::setAlwaysSendTo);
    setPath(baseDir, toml.getString("passwords"), memoryConfig::setPasswords);
    setString(toml.getString("storage"), memoryConfig::setStorage);
    setString(toml.getString("tls"), memoryConfig::setTls);
    setPath(baseDir, toml.getString("tlsservercert"), memoryConfig::setTlsServerCert);
    setPathArray(baseDir, toml.getList("tlsserverchain"), memoryConfig::setTlsServerChain);
    setPath(baseDir, toml.getString("tlsserverkey"), memoryConfig::setTlsServerKey);
    setString(toml.getString("tlsservertrust"), memoryConfig::setTlsServerTrust);
    setPath(baseDir, toml.getString("tlsknownclients"), memoryConfig::setTlsKnownClients);
    setPath(baseDir, toml.getString("tlsclientcert"), memoryConfig::setTlsClientCert);
    setPathArray(baseDir, toml.getList("tlsclientchain"), memoryConfig::setTlsClientChain);
    setPath(baseDir, toml.getString("tlsclientkey"), memoryConfig::setTlsClientKey);
    setString(toml.getString("tlsclienttrust"), memoryConfig::setTlsClientTrust);
    setPath(baseDir, toml.getString("tlsknownservers"), memoryConfig::setTlsKnownServers);

    // Validations
    if (memoryConfig.nodePort() == Integer.MIN_VALUE) {
      errorMsg.append("Error: value for key 'nodeport' in config must be specified\n");
    }

    if (memoryConfig.clientPort() == Integer.MIN_VALUE) {
      errorMsg.append("Error: value for key 'clientport' in config must be specified\n");
    }

    if (memoryConfig.clientPort() == memoryConfig.nodePort()) {
      errorMsg.append("Error: value for key 'nodeport' in config must be different to 'clientport'\n");
    }

    if (othernodesError.length() != 0) {
      errorMsg.append("Error: key 'othernodes' in config contains malformed URLS.\n");
      errorMsg.append(othernodesError);
    }

    if (memoryConfig.publicKeys().length != memoryConfig.privateKeys().length) {
      errorMsg.append("Error: the number of keys specified for keys 'publickeys' and 'privatekeys' must be the same\n");
    }

    if (!validateStorageTypes(memoryConfig.storage())) {
      errorMsg.append("Error: value for key 'storage' type must start with: ['leveldb', 'mapdb'] or be 'memory'\n");
    }

    if (!validateTLS(memoryConfig.tls())) {
      errorMsg.append("Error: value for key 'tls' status must be 'strict' or 'off'\n");
    }

    if (!validateTrustMode(memoryConfig.tlsServerTrust())) {
      errorMsg.append(
          "Error: value for key 'tlsservertrust' mode must must be one of ['whitelist', 'tofu', 'ca', 'ca-or-tofu', 'insecure-no-validation']\n");
    }

    if (!validateTrustMode(memoryConfig.tlsClientTrust())) {
      errorMsg.append(
          "Error: value for key 'tlsclienttrust' mode must must be one of ['whitelist', 'tofu', 'ca', 'ca-or-tofu', 'insecure-no-validation']\n");
    }

    if (errorMsg.length() != 0) {
      errorMsg.insert(0, "Invalid Configuration Options\n");
      throw new ConfigException(errorMsg.toString());
    }

    return memoryConfig;
  }

  private void setPath(Path baseDir, String value, Consumer<Path> setter) {
    if (value != null) {
      setter.accept(baseDir.resolve(value));
    }
  }

  private void setString(String value, Consumer<String> setter) {
    if (value != null) {
      setter.accept(value);
    }
  }

  private void setInt(Long value, Consumer<Integer> setter) {
    if (value != null) {
      setter.accept(toIntExact(value));
    }
  }

  private void setPathArray(Path baseDir, List<String> paths, Consumer<Path[]> setter) {
    if (paths != null) {
      setter.accept(paths.stream().map(baseDir::resolve).toArray(Path[]::new));
    }
  }

  private void setStringArray(List<String> paths, Consumer<String[]> setter) {
    if (paths != null) {
      setter.accept(paths.toArray(new String[paths.size()]));
    }
  }

  private StringBuilder setURLArray(List<String> urls, Consumer<URL[]> setter) {
    URL[] urlArray;
    StringBuilder errorMsg = new StringBuilder();

    if (urls != null) {
      urlArray = new URL[urls.size()];
      for (int i = 0; i < urls.size(); i++) {
        try {
          urlArray[i] = new URL(urls.get(i));
        } catch (MalformedURLException e) {
          errorMsg.append("\tURL [").append(urls.get(i)).append("] ").append(e.getMessage()).append("\n");
        }
      }
      setter.accept(urlArray);
    }

    return errorMsg;
  }

  // Validators
  // If options change, error message must also be changed
  boolean validateTrustMode(String mode) {
    List<String> validModes = Arrays.asList("whitelist", "tofu", "ca", "ca-or-tofu", "insecure-no-validation");
    return validModes.stream().anyMatch(mode::equals);
  }

  // If options change, error message must also be changed
  boolean validateStorageTypes(String storage) {
    return storage.startsWith("mapdb") || storage.startsWith("leveldb") || storage.equals("memory");
  }

  // If options change, error message must also be changed
  boolean validateTLS(String tls) {
    return tls.equals("strict") || tls.equals("off");
  }
}
