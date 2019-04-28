description: TLS 
<!--- END of page meta data -->

# TLS

Orion supports the TLS (Transport Layer Security) protocol to enable secure communications.

Enable TLS by setting the `tls` property to `strict` in the [Orion configuration file](Configuration-File.md).  

## Getting Started with TLS

To generate certificates and populate the [`tlsknownclients`](#tlsknownclients) and [`tlsknownservers`](#tlsknownservers)
files: 

1. Start the first Orion node with the following TLS properties specified in the [configuration file](Configuration-File.md): 
   ```bash
   tls = "strict"
   tlsservertrust="insecure-no-validation"
   tlsclienttrust="insecure-no-validation"    
   ```
   
    The following files are generated: 
   
    * [`tlsknownclients`](#tlsknownclients) and [`tlsknownservers`](#tlsknownservers)
    * `tls-client-cert.pem` and `tls-client-key.pem`
    * `tls-server-cert.pem` and `tls-server-key.pem`
   
2. Copy the generated files to the other Orion nodes. 

3. In the configuration files for the other Orion nodes, include the same TLS properties as the first node. 

4. Start all of the nodes. The `tls-known-servers` file for each node is updated to include the other nodes. 

5. Stop all of the nodes and update the TLS properties in the configuration file: 
   ```bash
   tls = "strict"
   tlsservertrust="whitelist"
   tlsclienttrust="whitelist"    
   ```
   
    When the nodes are restarted, only the nodes that have been started in the previous step can connect.  

## Configuring TLS 

TLS properties are specified in the [configuration file](Configuration-File.md). 

### tls 

TLS status options are:

* `strict` - All connections to and from this node must use TLS with mutual authentication. See [tlsservertrust](#tlsservertrust)
and [tlsclienttrust](#tlsclienttrust). 
* `off` - Mutually authenticated TLS is not used for in- and outbound connections. Unauthenticated 
connections to HTTPS hosts are still possible. Use only if another transport security mechanism like 
WireGuard is in place.

### tlsservercert

File containing the server TLS certificate in Apache format. The certificate identifies this
node to other nodes when they connect to the node API. If the certificate does not exist, it
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



