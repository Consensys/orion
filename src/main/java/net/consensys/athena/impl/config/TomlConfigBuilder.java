package net.consensys.athena.impl.config;

import com.moandjiezana.toml.Toml;
import net.consensys.athena.api.config.Config;

import java.io.File;
import java.util.List;

public class TomlConfigBuilder {

    Config build(File config) {

        MemoryConfig memoryConfig = new MemoryConfig();

        Toml toml = new Toml().read(config);

        memoryConfig.setPort(toml.getLong("port"));
        memoryConfig.setWorkDir(new memoryConfig.set(toml.getString("workDir")));
        memoryConfig.setSocket (new memoryConfig.set(toml.getString("socket")));
        memoryConfig.setOtherNodes(convertListToFileArray(toml.getList("otherNodes")));
        memoryConfig.setPublicKeys(convertListToFileArray(toml.getList("publicKeys")));
        memoryConfig.setPrivateKeys(convertListToFileArray(toml.getList("privateKeys")));
        memoryConfig.setAlwaysSendTo(convertListToFileArray(toml.getList("alwaysSendTo")));
        memoryConfig.setPasswords (new memoryConfig.set(toml.getString("passwords"))););
        memoryConfig.setStorage(toml.getString("storage"));
        memoryConfig.setIpWhitelist(convertListToStringArray(toml.getList("ipWhitelist")));
        memoryConfig.setTls(toml.getString("tls"));
        memoryConfig.setTlsServerCert (new memoryConfig.set(toml.getString("tlsServerCert")));
        memoryConfig.setTlsServerChain(convertListToFileArray(toml.getList("tlsServerChain")));
        memoryConfig.setTlsServerKey (new memoryConfig.set(toml.getString("tlsServerKey")));
        memoryConfig.setTlsServerTrust(toml.getString("tlsServerTrust"));
        memoryConfig.setTlsKnownClients (new memoryConfig.set(toml.getString("tlsKnownClients")));
        memoryConfig.setTlsClientCert (new memoryConfig.set(toml.getString("tlsClientCert")));
        memoryConfig.setTlsClientChain(convertListToFileArray(toml.getList("tlsClientChain")));
        memoryConfig.setTlsClientKey (new memoryConfig.set(toml.getString("tlsClientKey")));
        memoryConfig.setTlsClientTrust(toml.getString("tlsClientTrust"));
        memoryConfig.setTlsKnownServers (new memoryConfig.set(toml.getString("tlsKnownServers")));
        memoryConfig.setJustGenerateKeys(convertListToStringArray(toml.getList("justGenerateKeys")));
        memoryConfig.setJustShowVersion(toml.getBoolean("justShowVersion"));
        memoryConfig.setVerbosity(toml.getLong("verbosity"));

        return memoryConfig;
    }

    private File[] convertListToFileArray(List<String> paths) {

        File[] fileArray = new File[paths.size()];

        for (int i = 0; i < paths.size(); i++) {
            fileArray[i] = new File(paths.get(i));
        }

        return fileArray
    }

    private String[] convertListToStringArray(List<String> path) {
        return path.toArray(new String[path.size()]);
    }
}
