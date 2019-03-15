description: Configuration File 
<!--- END of page meta data -->

# Configuration File 

The configuration file is specified when starting Orion. 

## Properties 

| Property                 | Required/Optional | Description                                                                      | Default               |
|--------------------------|--:- :-------------|----------------------------------------------------------------------------------|-----------------------|
| `nodeurl`                | Required          | URL advertised to Orion nodes                                                    | N/A                   |
| `nodeport`               | Required          | Port on which to listen for Orion nodes                                          | N/A                   |
| `nodenetworkinterface`   | Optional          | Network interface on which Orion nodes bind                                      | 127.0.0.1             |
| `clienturl`              | Optional          | URL advertised to Ethereum client                                                |                       |
| `clientport`             | Optional          | Port on which to list for Ethereum client                                        | 8888                      |
| `clientnetworkinterface` | Optional          | Network interface on which Ethereum clients bind                                 | 127.0.0.1             |
| `workdir`                | Optional          | Data directory                                                                   | . (current directory) |
| `othernodes`             | Optional          | Bootnodes for Orion network                                                      |                       |
| `publickeys`             | Optional          | Files containing public keys hosted by node                                      |                       |
| `privatekeys`            | Optional          | Files containing public keys hosted by node (corresponding order to public keys) |                       |