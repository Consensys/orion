package net.consensys.athena.impl.config;

import net.consensys.athena.api.config.Config;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import com.moandjiezana.toml.Toml;

public class TomlConfigBuilder {

  Config build(InputStream config) {

    MemoryConfig memoryConfig = new MemoryConfig();

    Toml toml = new Toml().read(config);

    memoryConfig.setUrl(toml.getString("url"));
    memoryConfig.setPort(toml.getLong("port"));

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
    memoryConfig.setIpWhitelist(convertListToStringArray(toml.getList("ipwhitelist")));

    memoryConfig.setTls(toml.getString("tls", "strict"));
    memoryConfig.setTlsServerCert(new File(toml.getString("tlsservercert", "tls-server-cert.pem")));
    memoryConfig.setTlsServerChain(convertListToFileArray(toml.getList("tlsserverchain")));
    memoryConfig.setTlsServerKey(new File(toml.getString("tlsserverkey", "tls-server-key.pem")));
    memoryConfig.setTlsServerTrust(toml.getString("tlsservertrust", "tofu"));
    memoryConfig.setTlsKnownClients(
        new File(toml.getString("tlsknownclients", "tls-known-clients")));
    memoryConfig.setTlsClientCert(new File(toml.getString("tlsclientcert", "tls-client-cert.pem")));
    memoryConfig.setTlsClientChain(convertListToFileArray(toml.getList("tlsclientchain")));
    memoryConfig.setTlsClientKey(new File(toml.getString("tlsclientkey", "tls-client-key.pem")));
    memoryConfig.setTlsClientTrust(toml.getString("tlsclienttrust", "ca-or-tofu"));
    memoryConfig.setTlsKnownServers(
        new File(toml.getString("tlsknownservers", "tls-known-servers")));
    memoryConfig.setVerbosity(toml.getLong("verbosity", (long) 1));

    return memoryConfig;
  }

  private File[] convertListToFileArray(List<String> paths) {
    return paths == null ? new File[0] : paths.stream().map(File::new).toArray(File[]::new);
  }

  private String[] convertListToStringArray(List<String> paths) {
    return paths == null ? new String[0] : paths.toArray(new String[paths.size()]);
  }
}
