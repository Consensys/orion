package net.consensys.orion.impl.config;

import static java.lang.Math.toIntExact;

import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.config.ConfigException;
import net.consensys.orion.api.exception.OrionErrorCode;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
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

    if (toml.getString("privacyurl") != null) {
      try {
        memoryConfig.setPrivacyUrl(new URL(toml.getString("privacyurl")));
      } catch (MalformedURLException e) {
        errorMsg.append("Error: key 'privacyurl' in config is malformed.\n\t");
        errorMsg.append(e.getMessage()).append("\n");
      }
    } else {
      errorMsg.append("Error: value for key 'privacyurl' in config must be specified\n");
    }

    // reading and setting workDir first;
    String baseDir = toml.getString("workdir");
    if (baseDir != null) {
      memoryConfig.setWorkDir(new File(baseDir));
    }

    setInt(toml.getLong("port"), memoryConfig::setPort);
    setInt(toml.getLong("privacyport"), memoryConfig::setPrivacyPort);
    setFile(baseDir, toml.getString("socket"), memoryConfig::setSocket);
    setString(toml.getString("libsodiumpath"), memoryConfig::setLibSodiumPath);

    StringBuilder othernodesError = setURLArray(toml.getList("othernodes"), memoryConfig::setOtherNodes);

    setFileArray(baseDir, toml.getList("publickeys"), memoryConfig::setPublicKeys);
    setFileArray(baseDir, toml.getList("privatekeys"), memoryConfig::setPrivateKeys);
    setFileArray(baseDir, toml.getList("alwayssendto"), memoryConfig::setAlwaysSendTo);
    setFile(baseDir, toml.getString("passwords"), memoryConfig::setPasswords);
    setString(toml.getString("storage"), memoryConfig::setStorage);
    setStringArray(toml.getList("ipwhitelist"), memoryConfig::setIpWhitelist);
    setString(toml.getString("tls"), memoryConfig::setTls);
    setFile(baseDir, toml.getString("tlsservercert"), memoryConfig::setTlsServerCert);
    setFileArray(baseDir, toml.getList("tlsserverchain"), memoryConfig::setTlsServerChain);
    setFile(baseDir, toml.getString("tlsserverkey"), memoryConfig::setTlsServerKey);
    setString(toml.getString("tlsservertrust"), memoryConfig::setTlsServerTrust);
    setFile(baseDir, toml.getString("tlsknownclients"), memoryConfig::setTlsKnownClients);
    setFile(baseDir, toml.getString("tlsclientcert"), memoryConfig::setTlsClientCert);
    setFileArray(baseDir, toml.getList("tlsclientchain"), memoryConfig::setTlsClientChain);
    setFile(baseDir, toml.getString("tlsclientkey"), memoryConfig::setTlsClientKey);
    setString(toml.getString("tlsclienttrust"), memoryConfig::setTlsClientTrust);
    setFile(baseDir, toml.getString("tlsknownservers"), memoryConfig::setTlsKnownServers);
    setInt(toml.getLong("verbosity"), memoryConfig::setVerbosity);

    // Validations
    if (memoryConfig.port() == Integer.MIN_VALUE) {
      errorMsg.append("Error: value for key 'port' in config must be specified\n");
    }

    if (memoryConfig.privacyPort() == Integer.MIN_VALUE) {
      errorMsg.append("Error: value for key 'privacyport' in config must be specified\n");
    }

    if (memoryConfig.privacyPort() == memoryConfig.port()) {
      errorMsg.append("Error: value for key 'privacyport' in config must be different to 'port'\n");
    }

    if (othernodesError.length() != 0) {
      errorMsg.append("Error: key 'othernodes' in config containes malformed URLS.\n");
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

    if (!validateVerbosity(memoryConfig.verbosity())) {
      errorMsg.append("Error: value for key 'verbosity' must be within range 0 to 3\n");
    }

    if (errorMsg.length() != 0) {
      errorMsg.insert(0, "Invalid Configuration Options\n");
      throw new ConfigException(OrionErrorCode.CONFIGURATION_OPTION, errorMsg.toString());
    }

    return memoryConfig;
  }

  private void setFile(String baseDir, String value, Consumer<File> setter) {
    if (value != null) {
      setter.accept(new File(baseDir, value));
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

  private void setFileArray(String baseDir, List<String> paths, Consumer<File[]> setter) {
    if (paths != null) {
      setter.accept(paths.stream().map(p -> new File(baseDir, p)).toArray(File[]::new));
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
  boolean validateVerbosity(long verbosity) {
    return verbosity >= 0 && verbosity <= 3;
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
