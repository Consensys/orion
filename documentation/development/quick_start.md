# Quick Start Guide

## Dependencies

To run Orion, you need Java and Libsodium installed in your system.
For instruction on how to install Libsodium [click here](documentation/development/dependencies.md).

## Downloading and Running Orion

### Download binary
Download the latest binary from [Bintray](https://bintray.com/consensys/binaries/orion/_latestVersion) and extract the folder:
```
tar -xvzf orion*.tar.gz
cd orion-<version>/bin
```

### Generate keys
When generating keys, you must specify a name:
```
$ ./orion -g orion
```
This will generate `orion.key` and `orion.pub` files.

### Create config file
Create a file `orion.conf` and add the following properties:
```
nodeurl = "http://127.0.0.1:8080/"
nodeport = 8080
clienturl = "http://127.0.0.1:8888/"
clientport = 8888
publickeys = ["orion.pub"]
privatekeys = ["orion.key"]
tls = "off"
```

### Start Orion
Run Orion with the created config file:
```
$ ./orion orion.conf
```

### Check if Orion is up and running
The Upcheck method can be used to test if Orion is up and running:
```
$ curl http://localhost:8888/upcheck
$ I'm up!
```
### Send a payload
With only one node running, you can only send a payload to yourself:
```
$ curl -X POST \
  http://localhost:8888/send \
  -H 'Content-Type: application/json' \
  -d '{ 
        "payload": "SGVsbG8sIFdvcmxkIQ==",
        "from": "4xanJzyaDPcBVMUSwl/tLp+DbXzd3jF9MKk1yJuyewE=",
        "to": ["4xanJzyaDPcBVMUSwl/tLp+DbXzd3jF9MKk1yJuyewE="]
      }'

$ {"key":"LcF7I+UnR2XBdSxZesiYE/lTtxVfFeY4EvL9fDXb0Uo="}
```
The `from` and `to` values are the public key generated in a previous step (`orion.pub`).

### Receive a payload
Using the key from the Send method, we can receive the payload:
```
$ curl -X POST \
  http://localhost:8888/receive \
  -H 'Content-Type: application/json' \
  -d '{
        "key": "LcF7I+UnR2XBdSxZesiYE/lTtxVfFeY4EvL9fDXb0Uo=",
        "to": "4xanJzyaDPcBVMUSwl/tLp+DbXzd3jF9MKk1yJuyewE="
      }'

$ {"payload":"SGVsbG8sIFdvcmxkIQ=="}
```
Where `SGVsbG8sIFdvcmxkIQ==` is is `Hello, World!` in Base64. :)