# Configuring Orion

You can start Orion providing a config file:
```
orion foo.conf
```
Where `foo.conf` is a file in the current directory.

### Configuration file

All required properties have default values. However, it is recommended to set at least the
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

### Storage

By default, Orion relies on leveldb to store information.

#### LevelDB

```
storage = "leveldb:oriondb"
```

#### MapDB

Orion offers persistence using [MapDB](http://www.mapdb.org/).

```
storage = "mapdb:oriondb"
```

#### SQL

Orion supports working with relational databases.

* Add the SQL driver jar to the lib folder of the Orion installation
* Create a table in your database:

| Database | Create statement |
|---|---|
| MySQL | `create table store(key varbinary, value varbinary, primary key(key))` |
| PostgresQL | `create table store(key bytea, value bytea, primary key(key))` |

* Set storage to:

```
storage = "sql:jdbc:postgresql://localhost/oriondb"

```
