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
    memoryConfig.setWorkDir(new File(toml.getString("workdir")));
    memoryConfig.setSocket(new File(toml.getString("socket")));
    memoryConfig.setOtherNodes(convertListToFileArray(toml.getList("othernodes")));
    memoryConfig.setPublicKeys(convertListToFileArray(toml.getList("publickeys")));
    memoryConfig.setPrivateKeys(convertListToFileArray(toml.getList("privatekeys")));
    memoryConfig.setAlwaysSendTo(convertListToFileArray(toml.getList("alwayssendto")));
    memoryConfig.setPasswords(new File(toml.getString("passwords")));
    memoryConfig.setStorage(toml.getString("storage"));
    memoryConfig.setIpWhitelist(convertListToStringArray(toml.getList("ipwhitelist")));
    memoryConfig.setTls(toml.getString("tls"));
    memoryConfig.setTlsServerCert(new File(toml.getString("tlsservercert")));
    memoryConfig.setTlsServerChain(convertListToFileArray(toml.getList("tlsserverchain")));
    memoryConfig.setTlsServerKey(new File(toml.getString("tlsserverkey")));
    memoryConfig.setTlsServerTrust(toml.getString("tlsservertrust"));
    memoryConfig.setTlsKnownClients(new File(toml.getString("tlsknownclients")));
    memoryConfig.setTlsClientCert(new File(toml.getString("tlsclientcert")));
    memoryConfig.setTlsClientChain(convertListToFileArray(toml.getList("tlsclientchain")));
    memoryConfig.setTlsClientKey(new File(toml.getString("tlsclientkey")));
    memoryConfig.setTlsClientTrust(toml.getString("tlsclienttrust"));
    memoryConfig.setTlsKnownServers(new File(toml.getString("tlsknownservers")));
    memoryConfig.setVerbosity(toml.getLong("verbosity"));

    return memoryConfig;
  }

  private File[] convertListToFileArray(List<String> paths) {
    return paths.stream().map(File::new).toArray(File[]::new);
  }

  private String[] convertListToStringArray(List<String> path) {
    return path.toArray(new String[path.size()]);
  }
}
