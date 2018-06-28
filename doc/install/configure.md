# Configuring Orion

You can start Orion providing a config file:
```
orion foo.conf
```
Where `foo.conf` is a file in the current directory.

### Configuration file

The only required properties are `nodeurl` and `nodeport`. Although, it is recommended to set at least the
following properties:

| property name | description |
|---|---|
| nodeurl | The URL to advertise to other nodes (reachable by them) |
| nodeport | The local port to listen on for Orion nodes|
| nodenetworkinterface | The network interface to bind to for Orion nodes |
| clienturl | The URL to advertise to the Ethereum client (reachable by it) |
| clientport | The local port to listen on for a client |
| clientnetworkinterface | The network interface to bind to for a client node |
| workdir | The folder to put stuff in (default: .) |
| othernodes | "Boot nodes" to connect to to discover the network |
| publickeys | Public keys hosted by this node |
| privatekeys | Private keys hosted by this node (in corresponding order) |

Example config file:

```
nodeurl = "http://127.0.0.1:9001/"
nodeport = 9001
nodenetworkinterface = "127.0.0.1"
clienturl = "http://127.0.0.1:9002/"
clientport = 9002
clientnetworkinterface = "127.0.0.1"
workdir = "data"
othernodes = ["http://127.0.0.1:9000/"]
publickeys = ["foo.pub"]
privatekeys = ["foo.key"]
```

You can check all the available properties in the  
[`sample.conf`](https://github.com/ConsenSys/orion/blob/master/src/main/resources/sample.conf) file.