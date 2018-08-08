# Client API

The Client API is used by clients to interact with Orion to send and receive payloads and check if Orion is up and running.

The port used by the Client API is defined by the `clientport ` property (default: 8888).

## Summary

| Method  | Description |
| ------------- | ------------- |
| [/upcheck](#upcheck)  | Check if Orion is up and running. |
| [/send](#send) | Send a Base64 encoded payload to Orion. Returns a payload key. |
| [/receive](#receive)  | Receive a Base64 encoded payload from Orion using the payload key.  |
| [/sendRaw](#send-raw ) | Send an arbitrary payload to Orion. Returns a payload key. |
| [/receiveRaw](#receive-raw) | Receive a payload from Orion using the payload key. |

## Upcheck
This method checks if Orion is up and running.

**HTTP Verb:** GET

**Headers:** none

**Request body:** none

**Response body (String):**
* "I'm up" string

**Example:**
```
$ curl -X GET http://127.0.0.1:8888/upcheck

$ I'm up
```

## Send
This method is used by a client to send a payload to Orion.

**HTTP Verb:** POST

**Headers:**
* Content-Type: application/json

**Request body (JSON):**
* payload (String): the Base64 encoded payload
* from (String): the public key of the sender
* to (Array of Strings): the list of public keys of the receivers of this payload

**Response body (JSON):**
* key (String): the key that should be used to receive the payload

**Example:**
```
$ curl -X POST http://127.0.0.1:8888/send \
  -H 'Content-Type: application/json' \
  -d '{
	"payload": "SGVsbG8sIFdvcmxkIQ==",
	"from": "4xanJzyaDPcBVMUSwl/tLp+DbXzd3jF9MKk1yJuyewE=",
	"to": ["YE5cJRJYTRO4XFo7yuAi/0K9DwjySGjsHB2YrFPnJXo="]
      }'

$ {"key":"wS+RMprLKIuCaHzOBfPeHmkJWUdOJ7Ji/9U3qj2jbXQ="}
```

## Receive
This method is used by a client to receive a payload from Orion using the payload key.

**HTTP Verb:** POST

**Headers:**
* Content-Type: application/json

**Request body (JSON):**
* key (String): the key of the payload
* to (String): the public key of the receiver

**Response body (JSON):**
* payload (String): the Base64 encoded payload

**Example:**
```
$ curl -X POST http://127.0.0.1:8888/receive \
  -H 'Content-Type: application/json' \
  -d '{
	"key": "wS+RMprLKIuCaHzOBfPeHmkJWUdOJ7Ji/9U3qj2jbXQ=",
	"to": "YE5cJRJYTRO4XFo7yuAi/0K9DwjySGjsHB2YrFPnJXo="
      }'

$ {"payload":"SGVsbG8sIFdvcmxkIQ=="}
```

## Send Raw
This method is used by a client to send a raw payload to Orion.

**HTTP Verb:** POST

**Headers:**
* Content-Type: application/octet-stream
* c11n-from: The sender's public key
* c11n-to: A list of receiver's public keys (separated by commas)

**Request body:**
* A payload

**Response body (String):**
* A string representing the payload key

**Example:**
```
$ curl -X POST http://127.0.0.1:8888/sendraw \
  -H 'Content-Type: application/octet-stream' \
  -H 'c11n-from: 4xanJzyaDPcBVMUSwl/tLp+DbXzd3jF9MKk1yJuyewE=' \
  -H 'c11n-to: YE5cJRJYTRO4XFo7yuAi/0K9DwjySGjsHB2YrFPnJXo=' \
  -d 'Hello, World!'

$ +3gnwO0oHXe4kXsr3kegd9jTTqsq3Y6Hm3w26WHR/RM=
```

## Receive Raw
This method is used by a client to receive a payload from Orion using the payload key.

**HTTP Verb:** POST

**Headers:**
* Content-Type: application/octet-stream
* c11n-key: the receiver's public key

**Request body:** none

**Response body:**
The payload

**Example:**
```
$ curl -X POST \
  http://127.0.0.1:8888/receiveraw \
  -H 'Content-Type: application/octet-stream' \
  -H 'c11n-key: +3gnwO0oHXe4kXsr3kegd9jTTqsq3Y6Hm3w26WHR/RM='

$ Hello, World!
```