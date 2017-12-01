package net.consensys.athena.api.config;

import java.io.File;
import java.util.Optional;

public interface Config {

    /**
     * @return URL for this node (what's advertised to other nodes, e.g. https://constellation.mydomain.com/)
     **/
    String getUrl();

    /**
     * @return Port to listen on for the public API
     **/
    long getPort();

    /**
     * @return Working directory to use (relative paths specified for other options are relative to the working directory)
     **/
    Optional<File> getWorkDir();

    /**
     * @return Path to IPC socket file to create for private API access
     **/

    Optional<File> getSocket();

    /**
     * @return Array of other node URLs to connect to on startup (this list may be incomplete)
     **/
    File[] getOtherNodes();

    /**
     * @return Array of paths to public keys to advertise
     **/
    File[] getPublicKeys();

    /**
     * @return Array of paths to corresponding private keys (these must be given in the same order as --publickeys)
     **/
    File[] getPrivateKeys();

    /**
     * @return Array of paths to public keys that are always included as recipients (these must be advertised somewhere)
     **/
    File[] getAlwaysSendTo();

    /**
     * @return A file containing the passwords for the specified --privatekeys, one per line, in the same order (if one key is not locked, add an empty line)
     **/
    Optional<File> getPasswords();

    /**
     * @return Storage string specifying a storage engine and/or storage path
     **/
    String getStorage();

    /**
     * @return Array of IPv4 and IPv6 addresses that may connect to this node's public API
     **/
    String[] getIpWhitelist();

    /**
     * @return TLS status (strict, off)
     **/
    String getTls();

    /**
     * @return TLS certificate file to use for the public API
     **/
    File getTlsServerCert();

    /**
     * @return Array of TLS chain certificates to use for the public API
     **/
    File[] getTlsServerChain();

    /**
     * @return TLS key to use for the public API
     **/
    File getTlsServerKey();

    /**
     * @return TLS server trust mode (whitelist, ca-or-tofu, ca, tofu, insecure-no-validation)
     **/
    String getTlsServerTrust();

    /**
     * @return TLS server known clients file for the ca-or-tofu, tofu and whitelist trust modes
     **/
    File getTlsKnownClients();

    /**
     * @return TLS client certificate file to use for connections to other nodes
     **/
    File getTlsClientCert();

    /**
     * @return Array of TLS chain certificates to use for connections to other nodes
     **/
    File[] getTlsClientChain();

    /**
     * @return TLS key to use for connections to other nodes
     **/
    File getTlsClientKey();

    /**
     * @return TLS client trust mode (whitelist, ca-or-tofu, ca, tofu, insecure-no-validation)
     **/
    String getTlsClientTrust();

    /**
     * @return TLS client known servers file for the ca-or-tofu, tofu and whitelist trust modes
     **/
    File getTlsKnownServers();

    /**
     * @return Array of key pair names to generate
     **/
    String[] getJustGenerateKeys();

    /**
     * @return Output current version information flag
     */
    boolean getJustShowVersion();

    /**
     * @return Level of verbosity, that is print more detailed information
     */
    long getVerbosity();


}
