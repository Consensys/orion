package net.consensys.athena.impl.config;

import net.consensys.athena.api.config.Config;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import com.moandjiezana.toml.Toml;

public class TomlConfigBuilder {

  Config build(InputStream config) {

    MemoryConfig memoryConfig = new MemoryConfig();

    //Toml toml = new Toml().read(config);
      Toml toml = new Toml().read(config);

    memoryConfig.setPort(toml.getLong("port"));
   // memoryConfig.setWorkDir(new File(toml.getString("workDir")));
   // memoryConfig.setSocket(new File(toml.getString("socket")));
   // memoryConfig.setOtherNodes(convertListToFileArray(toml.getList("otherNodes")));
   // memoryConfig.setPublicKeys(convertListToFileArray(toml.getList("publicKeys")));
   // memoryConfig.setPrivateKeys(convertListToFileArray(toml.getList("privateKeys")));
   // memoryConfig.setAlwaysSendTo(convertListToFileArray(toml.getList("alwaysSendTo")));
   // memoryConfig.setPasswords(new File(toml.getString("passwords")));
    memoryConfig.setStorage(toml.getString("storage"));
   // memoryConfig.setIpWhitelist(convertListToStringArray(toml.getList("ipWhitelist")));
    memoryConfig.setTls(toml.getString("tls"));
   // memoryConfig.setTlsServerCert(new File(toml.getString("tlsServerCert")));
  //  memoryConfig.setTlsServerChain(convertListToFileArray(toml.getList("tlsServerChain")));
   // memoryConfig.setTlsServerKey(new File(toml.getString("tlsServerKey")));
   // memoryConfig.setTlsServerTrust(toml.getString("tlsServerTrust"));
   // memoryConfig.setTlsKnownClients(new File(toml.getString("tlsKnownClients")));
   // memoryConfig.setTlsClientCert(new File(toml.getString("tlsClientCert")));
   // memoryConfig.setTlsClientChain(convertListToFileArray(toml.getList("tlsClientChain")));
   // memoryConfig.setTlsClientKey(new File(toml.getString("tlsClientKey")));
    memoryConfig.setTlsClientTrust(toml.getString("tlsClientTrust"));
   // memoryConfig.setTlsKnownServers(new File(toml.getString("tlsKnownServers")));
   // memoryConfig.setJustGenerateKeys(convertListToStringArray(toml.getList("justGenerateKeys")));
   // memoryConfig.setJustShowVersion(toml.getBoolean("justShowVersion"));
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
