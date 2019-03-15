description: Configuration File 
<!--- END of page meta data -->

# Configuration File 

The configuration file is specified when starting Orion. 

## Properties 

| Property                 | Required/Optional | Description                                                                      | Default               |
|--------------------------|--:- :-------------|----------------------------------------------------------------------------------|-----------------------|
| `nodeurl`                | Required          | URL advertised to Orion nodes                                                    | nodeurl = "http://127.0.0.1:8080/"                   |
| `nodeport`               | Required          | Port on which to listen for Orion nodes                                          | 8080                   |
| `nodenetworkinterface`   | Optional          | Host on which to listen Orion nodes                                              | "127.0.0.1"           |
| `clienturl`              | Optional          | URL advertised to Ethereum client                                                | "http://127.0.0.1:8888" |
| `clientport`             | Optional          | Port on which to list for Ethereum client                                        | 8888                  |
| `clientnetworkinterface` | Optional          | Host on which to listen for Ethereum clients                                     | "127.0.0.1"             |
| `workdir`                | Optional          | Data directory                                                                   | . (current directory) |
| `othernodes`             | Optional          | Bootnodes for Orion network                                                      | [] (empty list)       |
| `publickeys`             | Optional          | List of files containing public keys hosted by node                                      | [] (empty list)       |
| `privatekeys`            | Optional          | List of files containing private keys hosted by node (corresponding order to public keys) | [] (empty list)       |
| `libsodiumpath`          | Optional          | Path to libsodium shared library                                                 | [Dependant on OS](#libsodium)
| `alwayssendto`           | Optional          | List of files containing public keys to include as recipients for every transaction| [] 
| `passwords`              | Optional          | File containing [passwords to unlock `privatekeys`](#passwords)                  | Not set
| `storage`                | Optional          | [Storage](#storage) for payloads and related information                         | "leveldb" 
| `ipwhitelist`            | Optional          | [IP whitelist](#ip) for Orion node API                                                | []

### libsodiumpath

Depends on the operational system, check the class LibSodiumSettings for more details. 

### alwayssendto

Used to specify list of files containing public keys to include as a recipient for every transaction sent
through the node (for example, for backup purposes). The specified public keys must be advertised by an 
Orion node on the network. That is, there must be Orion nodes with the specified public keys included in their
`publickeys` list. 

### passwords

File contains one password per line. Include an empty line for keys that ar not locked. 

### storage

Storage for payloads and related information. Options are:

* `leveldb:path` - LevelDB
* `mapdb:path` - MapDB
* `memory` - Contents cleared when Orion exits.

### ipwhitelist

If unspecified/empty, connections from all sources will be allowed (but the private API remains accessible only
via the IPC socket.) To allow connections from localhost when a whitelist is defined when running multiple Orion
nodes on the same machine, add 127.0.0.1 and ::1 to this list.

## TLS status. Options:
##
##   - strict: All connections to and from this node must use TLS with mutual
##       authentication. See the documentation for 'tlsservertrust' and 'tlsclienttrust'
##   - off: Mutually authenticated TLS is not used for in- and outbound
##       connections, although unauthenticated connections to HTTPS hosts are still possible. This
##       should only be used if another transport security mechanism like WireGuard is in place.
##
## Default: "strict"
tls = "strict"

## containing the server's TLS certificate in Apache format. This is used to identify this
## node to other nodes in the network when they connect to the public API. If it doesn't exist it
## will be created.
##
## Default: "tls-server-cert.pem"
tlsservercert = "tls-server-cert.pem"

## List of files that constitute the CA trust chain for the server certificate. This can be empty
## for auto-generated/non-PKI-based certificates.
##
## Default: []
tlsserverchain = []

## The private key for the server TLS certificate. If the doesn't exist it will be
## created.
##
## Default: "tls-server-key.pem"
tlsserverkey = "tls-server-key.pem"

## TLS trust mode for the server. This decides who's allowed to connect to it. Options:
##
##   - whitelist: Only nodes that have previously connected to this node and
##       been added to the 'tlsknownclients' will be allowed to connect. This mode will
##       not add any new clients to the 'tlsknownclients' file.
##   - tofu: (Trust-on-first-use) Only the first node that connects identifying
##       as a certain host will be allowed to connect as the same host in the future. Note that
##       nodes identifying as other hosts will still be able to connect - switch to whitelist
##       after populating the 'tlsknownclients' list to restrict access.
##   - ca: Only nodes with a valid certificate and chain of trust to one of the
##       system root certificates will be allowed to connect. The folder containing trusted root
##       certificates can be overridden with the SYSTEM_CERTIFICATE_PATH environment variable.
##   - ca-or-tofu: A combination of ca and tofu: If a certificate is valid, it
##       is always allowed and added to the 'tlsknownclients' list. If it is self-signed, it
##       will be allowed only if it's the first certificate this node has seen for that host.
##   - insecure-no-validation: Any client can connect, however they will still
##       be added to the 'tlsknownclients' file.
##
## Default: "tofu"
tlsservertrust = "tofu"

## TLS known clients for the server. This contains the fingerprints of public keys of other
## nodes that are allowed to connect to this one for the ca-or-tofu, tofu and whitelist trust
## modes
##
## Default: "tls-known-clients"
tlsknownclients = "tls-known-clients"

## containing the client's TLS certificate in Apache format. This is used to identify this
## node to other nodes in the network when it is connecting to their public APIs. If it doesn't
## exist it will be created
##
## Default: "tls-client-cert.pem"
tlsclientcert = "tls-client-cert.pem"

## List of files that constitute the CA trust chain for the client certificate. This can be empty
## for auto-generated/non-PKI-based certificates.
##
## Default: []
tlsclientchain = []

## The private key for the client TLS certificate. If it doesn't exist it will be created.
##
## Default: "tls-client-key.pem"
tlsclientkey = "tls-client-key.pem"

## TLS trust mode for the client. This decides which servers it will connect to. Options:
##
##   - whitelist: This node will only connect to servers it has previously seen
##       and added to the 'tlsknownservers' file. This mode will not add any new servers to
##       the 'tlsknownservers' file.
##   - tofu: (Trust-on-first-use) This node will only connect to the same
##       server for any given host. (Similar to how OpenSSH works.)
##   - ca: The node will only connect to servers with a valid certificate and
##       chain of trust to one of the system root certificates. The folder containing trusted root
##       certificates can be overridden with the SYSTEM_CERTIFICATE_PATH environment variable.
##   - ca-or-tofu: A combination of ca and tofu: If a certificate is valid, it
##       is always allowed and added to the 'tlsknownservers' list. If it is self-signed, it
##       will be allowed only if it's the first certificate this node has seen for that host.
##   - insecure-no-validation: This node will connect to any server, regardless
##       of certificate, however it will still be added to the 'tlsknownservers' file.
##
## Default: "ca-or-tofu"
tlsclienttrust = "ca-or-tofu"

## TLS known servers for the client. This contains the fingerprints of public keys of other
## nodes that this node has encountered for the ca-or-tofu, tofu and whitelist trust modes.
##
## Default: "tls-known-servers"
tlsknownservers = "tls-known-servers"
