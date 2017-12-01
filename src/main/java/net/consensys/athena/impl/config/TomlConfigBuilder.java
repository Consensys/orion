package net.consensys.athena.impl.config;

import com.moandjiezana.toml.Toml;
import net.consensys.athena.api.config.Config;

import java.io.File;
import java.util.List;

public class TomlConfigBuilder {

    Config build(File config) {
        Toml toml = new Toml().read(config);

        String url = toml.getString("url");
        long port = toml.getLong("port");
        File workDir = new File (toml.getString("workDir"));
        File socket  = new File (toml.getString("socket"));
        File[] otherNodes = convertListToFileArray(toml.getList("otherNodes"));
        File[] publicKeys = convertListToFileArray(toml.getList("publicKeys"));
        File[] privateKeys = convertListToFileArray(toml.getList("privateKeys"));
        File[] alwaysSendTo = convertListToFileArray(toml.getList("alwaysSendTo"));
        File passwords  = new File (toml.getString("passwords"));;
        String storage = toml.getString("storage");

        String[] ipWhitelist = convertListToStringArray(toml.getList("ipWhitelist"));

        String tls = toml.getString("tls");
        File tlsServerCert  = new File (toml.getString("tlsServerCert"));
        File[] tlsServerChain = convertListToFileArray(toml.getList("tlsServerChain"));
        File tlsServerKey  = new File (toml.getString("tlsServerKey"));
        String tlsServerTrust = toml.getString("tlsServerTrust");
        File tlsKnownClients  = new File (toml.getString("tlsKnownClients"));
        File tlsClientCert  = new File (toml.getString("tlsClientCert"));
        File[] tlsClientChain = convertListToFileArray(toml.getList("tlsClientChain"));
        File tlsClientKey  = new File (toml.getString("tlsClientKey"));
        String tlsClientTrust = toml.getString("tlsClientTrust");
        File tlsKnownServers  = new File (toml.getString("tlsKnownServers"));

        String[] justGenerateKeys = convertListToStringArray(toml.getList("justGenerateKeys"));

        boolean justShowVersion = toml.getBoolean("justShowVersion");
        long verbosity = toml.getLong("verbosity");

        return new MemoryConfig(url, port, workDir, socket, otherNodes, publicKeys, privateKeys,
                alwaysSendTo, passwords, storage, ipWhitelist, tls, tlsServerCert, tlsServerChain, tlsServerKey,
                tlsServerTrust, tlsKnownClients, tlsClientCert, tlsClientChain, tlsClientKey, tlsClientTrust,
                tlsKnownServers, justGenerateKeys, justShowVersion, verbosity);

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
