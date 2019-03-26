# Getting Started

## Prequisites

[Orion](../Installation/Install-Binaries.md)

## Starting Orion and Sending a Payload

### 1. Create Password File

Create a file containing the password used to encrypt the key pair. 

The password is specified when [generating the keys](#2-generate-keys) and the file is specified for the `passwords` property in the 
[configuration file](#3-create-a-configuration-file). 

### 2. Generate Keys

To generate a public/private keypair for the Orion node:

``` bash
orion -g nodeKey
```

At the prompt, enter the [password](#1-create-password-file) to encrypt the key pair. 

The public/private key pair is generated and the keys saved in the `nodeKey.pub` and `nodeKey.key` files.

### 3. Create a Configuration File

Create a file called `orion.conf` and add the following properties:

```
nodeurl = "http://127.0.0.1:8080/"
nodeport = 8080
clienturl = "http://127.0.0.1:8888/"
clientport = 8888
publickeys = ["nodeKey.pub"]
privatekeys = ["nodeKey.key"]
passwords = "/<pathToPasswordFile>"/passwordFile
tls = "off"
```

### 4. Start Orion

Start Orion specifying the [configuration file](#3-create-a-configuration-file):

```
orion orion.conf
```

### 5. Confirm Orion is Running

Use the `upcheck` method to confirm Orion is up and running:

```bash tab="Request"
curl http://localhost:8888/upcheck
```

```bash tab="Result"
I'm up!
```

### 6. Send a Payload

With one node running, send a payload to yourself where the `from` and `to` values are the [generated public key](#2-generate-keys) (`nodeKey.pub`):

```bash tab="Request"
curl -X POST \
http://localhost:8888/send \
-H 'Content-Type: application/json' \
-d '{ 
      "payload": "SGVsbG8sIFdvcmxkIQ==",
      "from": "<OrionPublicKey>",
      "to": ["<OrionPublicKey>"]
    }'
```

```bash tab="Example"
curl -X POST \
http://localhost:8888/send \
-H 'Content-Type: application/json' \
-d '{ 
      "payload": "SGVsbG8sIFdvcmxkIQ==",
      "from": "4xanJzyaDPcBVMUSwl/tLp+DbXzd3jF9MKk1yJuyewE=",
      "to": ["4xanJzyaDPcBVMUSwl/tLp+DbXzd3jF9MKk1yJuyewE="]
    }'
```

```bash tab="Result"
{"key":"LcF7I+UnR2XBdSxZesiYE/lTtxVfFeY4EvL9fDXb0Uo="}
```

### 7. Receive a Payload

Use the key received when [sending the payload](#6-send-a-payload) to receive the payload:

```bash tab="Request"
curl -X POST \
http://localhost:8888/receive \
-H 'Content-Type: application/json' \
-d '{
      "key": "<KeyReceivedFromSendMethod>",
      "to": "<OrionPublicKey>"
    }'
```

```bash tab="Example"
curl -X POST \
http://localhost:8888/receive \
-H 'Content-Type: application/json' \
-d '{
      "key": "LcF7I+UnR2XBdSxZesiYE/lTtxVfFeY4EvL9fDXb0Uo=",
      "to": "4xanJzyaDPcBVMUSwl/tLp+DbXzd3jF9MKk1yJuyewE="
    }'
```

```bash tab="Result"
{"payload":"SGVsbG8sIFdvcmxkIQ=="}
```

Where `SGVsbG8sIFdvcmxkIQ==` is `Hello, World!` in Base64.
