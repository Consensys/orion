description: Configuration File 
<!--- END of page meta data -->

# Configuration File 

Specify the configuration file when [starting Orion](../Reference/Orion-CLI-Syntax.md#configuration-file). 

## Properties 

| Property                 | Required | Description                                                                      | Default               |
|--------------------------|--:- :-------------|----------------------------------------------------------------------------------|-----------------------|
| `nodeurl`                | Required          | URL advertised to Orion nodes                                                    | `"http://127.0.0.1:8080/"`                   |
| `nodeport`               | Required          | Port on which to listen for Orion nodes                                          | `8080`                   |
| `nodenetworkinterface`   | Optional          | Host on which to listen for Orion nodes                                              | `"127.0.0.1"`           |
| `clienturl`              | Optional          | URL advertised to Ethereum clients                                                | `"http://127.0.0.1:8888"` |
| `clientport`             | Optional          | Port on which to listen for Ethereum clients                                        | `8888`                  |
| `clientnetworkinterface` | Optional          | Host on which to listen for Ethereum clients                                     | `"127.0.0.1"`             |
| `workdir`                | Optional          | Data directory                                                                   | `. (current directory)` |
| `othernodes`             | Optional          | Bootnodes for Orion network                                                      | `[] (empty list)`       |
| `publickeys`             | Optional          | List of files containing public keys hosted by node                                     | `[] (empty list)`
| `privatekeys`            | Optional          | List of files containing private keys hosted by node (corresponding order to public keys) | `[] (empty list)`       |
| `libsodiumpath`          | Optional          | Path to libsodium shared library                                                 | [Dependant on OS](#libsodiumpath)
| `alwayssendto`           | Optional          | List of files containing public keys to include as recipients for every transaction| `[]` 
| `passwords`              | Optional          | File containing [passwords](#passwords) to unlock `privatekeys`                      | `Not set`
| `storage`                | Optional          | [Storage](#storage) for payloads and related information                         | `"leveldb"` 
| `tls`                    | Optional          | [TLS status options](#tls)                                                       |  `"strict"`
| `tlsservercert`          | Optional          | [Server TLS certificate](#tlsservercert)                                         | `"tls-server-cert.pem"`
| `tlsserverchain`         | Optional          | [Files that make up the CA trust chain](#tlsserverchain)                         | `[]`
| `tlsserverkey`           | Optional          | [Private key for the server TLS certificate](#tlsserverkey)                      | `"tls-server-key.pem"`
| `tlsservertrust`         | Optional          | [TLS trust mode for the server](#tlsservertrust)                                 | `"tofu"`
| `tlsknownclients`        | Optional          | [TLS known clients for the server](#tlsknownclients)                             | `"tls-known-clients"`
| `tlsclientcert`          | Optional          | [Client TLS certificate](#tlsclientcert)                                         | `"tls-client-cert.pem"`
| `tlsclientchain`         | Optional          | [Files that make up the CA trust chain](#tlsclientchain)                         | `[]`
| `tlsclientkey`           | Optional          | [Private key for the client TLS certificate](#tlsclientkey)                      | `"tls-client-key.pem"`
| `tlsclienttrust`         | Optional          | [TLS trust mode for the client](#tlsclienttrust)                                 | `"ca-or-tofu"`
| `tlsknownservers`        | Optional          | [TLS known servers for the client](#tlsknownservers)                             | `"tls-known-servers"`

### libsodiumpath

Depends on the operational system, check the class LibSodiumSettings for more details. 

### alwayssendto

Specifies list of files containing public keys to include as a recipient for every transaction sent
through the node (for example, for backup purposes). The specified public keys must be advertised by an 
Orion node on the network. That is, there must be an Orion node with the specified public keys included in the node
`publickeys` list. 

### passwords

File contains one password per line. Include an empty line for keys that are not locked. 

### tls 

TLS status options are:

* `strict` - All connections to and from this node must use TLS with mutual authentication. See [tlsservertrust](#tlsservertrust)
and [tlsclienttrust](#tlsclienttrust). 
* `off` - Mutually authenticated TLS is not used for in- and outbound connections, although unauthenticated 
connections to HTTPS hosts are still possible. Use only if another transport security mechanism like 
WireGuard is in place.

### tlsservercert

File containing the server TLS certificate in Apache format. The certificate identifies this
node to other nodes when they connect to the node API. If the certificate does not exist it
is created.

### tlsserverchain

List of files that make up the CA trust chain for the server certificate. The list can be empty for auto-generated/non-PKI-based 
certificates.

### tlsserverkey

File containing the private key for the server TLS certificate. If the private key does not exist, it is
created. 

### tlsservertrust

TLS trust mode for the server. The trust mode defines which nodes can connect to the server. Options:

* `whitelist` - Only nodes that have previously connected to this node and have been added to `tlsknownclients`
 can connect. New clients are not added to `tlsknownclients`.
 
* `tofu` - Trust-on-first-use. Only the first node that connects identifying as a certain host can connect
 as the same host in the future. Nodes identifying as other hosts can still connect. To restrict access, change
 the mode to `whitelist` after populating the `tlsknownclients` list.

* `ca` -  Only nodes with a valid certificate and chain of trust to one of the system root certificates 
can connect.  Use the `SYSTEM_CERTIFICATE_PATH` environment variable to override the directory containing
 trusted root certificates.

* `ca-or-tofu` - Combination of `ca` and `tofu`. If a certificate is valid, it is always allowed and added 
to the `tlsknownclients` list. If it is self-signed, it is allowed only if it is the first certificate 
this node has seen for that host.

* `insecure-no-validation` - Any client can connect. Clients are added to the `tlsknownclients` file.

### tlsknownclients

TLS known clients for the server. The `tlsknownclients` contains the fingerprints of public keys of other
nodes that can connect to this node for the `ca-or-tofu`, `tofu`, and `whitelist` trust modes.

### tlsclientcert

File containing the client TLS certificate in Apache format. The certificate identifies this
node to other nodes when it is connecting to the node API. If the certificate does not
exist, it is created.

### tlsclientchain

List of files that make up the CA trust chain for the client certificate. The list can be empty for auto-generated/non-PKI-based 
certificates.

### tlsclientkey

File containing the private key for the client TLS certificate. If the private key does not exist, it is
created.

### tlsclienttrust

TLS trust mode for the client. The trust mode defines the servers to which the client connects. Options:

* `whitelist` - Nodes only connects to servers it has previously seen and have been added to `tlsknownservers`. 
New servers are not added to `tlsknownservers`.
 
* `tofu` - Trust-on-first-use. Node only connects same server for any given host. This is similar to how
OpenSSH works. 

* `ca` -  Node only connects to servers with a valid certificate and chain of trust to one of the system 
root certificates. Use the `SYSTEM_CERTIFICATE_PATH` environment variable to override the directory containing
 trusted root certificates.

* `ca-or-tofu` - Combination of `ca` and `tofu`. If a certificate is valid, it is always allowed and added 
to the `tlsknownservers` list. If it is self-signed, it is allowed only if it is the first certificate 
this node has seen for that host.

* `insecure-no-validation` - Node connects to any server. Servers are added to the `tlsknownservers` file.

### tlsknownservers 

TLS known servers for the client. The `tlsknownservers` contains the fingerprints of public keys of other
nodes that this node has encountered for the `ca-or-tofu`, `tofu`, and `whitelist` trust modes.


