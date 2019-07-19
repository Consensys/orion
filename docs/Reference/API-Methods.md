# Client API

The Client API is used by Ethereum clients (for example, Pantheon) to interact with Orion.

The port used by the Client API is defined by the `clientport ` property in the [configuration file](../Configuring-Orion/Configuration-File.md). 
The default port is `8888`.

## send

Sends a payload to Orion.

**HTTP Verb**
POST 

**Headers:**
Content-Type: application/json

**Request Body**

`payload` : *string* - Base64 encoded payload

`from` : *string*  - Public key of sender

`to` or `privacyGroupId` to receive this payload

Where: 

`to` : *array of strings* - List of public keys 

`privacyGroupId` : *string* - Privacy group ID of existing privacy group

**Returns**

`key` : *string* - Key used to receive the payload

!!! example
    ```bash tab="curl HTTP request"
    curl -X POST http://127.0.0.1:8888/send \
      -H 'Content-Type: application/json' \
      -d '{
    	"payload": "SGVsbG8sIFdvcmxkIQ==",
    	"from": "4xanJzyaDPcBVMUSwl/tLp+DbXzd3jF9MKk1yJuyewE=",
    	"to": ["YE5cJRJYTRO4XFo7yuAi/0K9DwjySGjsHB2YrFPnJXo="]
    }'
    ```
    
    ```bash tab="With Privacy Group ID"
        curl -X POST http://127.0.0.1:8888/send \
          -H 'Content-Type: application/json' \
          -d '{
        	"payload": "SGVsbG8sIFdvcmxkIQ==",
        	"from": "4xanJzyaDPcBVMUSwl/tLp+DbXzd3jF9MKk1yJuyewE=",
        	"privacyGroupId": "kAbelwaVW7okoEn1+okO+AbA4Hhz/7DaCOWVQz9nx5M="
        }'
    ```
   
    ```json tab="Result"
    {"key":"wS+RMprLKIuCaHzOBfPeHmkJWUdOJ7Ji/9U3qj2jbXQ="}
    ```

## receive

Receives a payload from Orion using the payload key. The payload key is returned by the [send](#send) method.

**HTTP Verb**
POST 

**Headers:**
Content-Type: application/json

**Request Body**

`key` : *string* - Key used to receive the payload

`to` : *string* - Public key of the receiver

**Returns**

`payload` : *string* - Base64 encoded payload

!!! example
    ```bash tab="curl HTTP request"
    curl -X POST http://127.0.0.1:8888/receive \
      -H 'Content-Type: application/json' \
      -d '{
    	"key": "wS+RMprLKIuCaHzOBfPeHmkJWUdOJ7Ji/9U3qj2jbXQ=",
    	"to": "YE5cJRJYTRO4XFo7yuAi/0K9DwjySGjsHB2YrFPnJXo="
    }'
    ```
   
    ```json tab="Result"
     {"payload":"SGVsbG8sIFdvcmxkIQ=="}
    ```

### Privacy Group ID 

To return the Privacy Group ID with the payload, use the `receive` method with the header `Content-Type: application/vnd.orion.v1+json`. 

!!! example
    ```bash tab="curl HTTP request"
    curl -X POST \
      http://127.0.0.1:8888/receive \
      -H 'Content-Type: application/vnd.orion.v1+json' \
      -d '{
    	"key": "X0iCPeAy8I/+IUeq13X1ozdVH5AHL6ISwmLXk6nPkPo=",
    	"to": "negmDcN2P4ODpqn/6WkJ02zT/0w0bjhGpkZ8UP6vARk="
    }'
    ```

    ```json tab="Result"
     {
         "payload": "SGVsbG8sIFdvcmxkIQ==",
         "privacyGroupId": "68/Cq0mVjB8FbXDLE1tbDRAvD/srluIok137uFOaClM="
     }
    ```

## sendraw

Sends a raw payload to Orion.

**HTTP Verb**
POST 

**Headers:**
Content-Type: application/octet-stream
c11n-from: Public key of the sender
c11n-to: List of public keys to receive this payload

**Request Body**

`payload` : *string* - Payload

**Returns**

Key used to receive the payload

!!! example
    ```bash tab="curl HTTP request"
    curl -X POST http://127.0.0.1:8888/sendraw \
      -H 'Content-Type: application/octet-stream' \
      -H 'c11n-from: 4xanJzyaDPcBVMUSwl/tLp+DbXzd3jF9MKk1yJuyewE=' \
      -H 'c11n-to: YE5cJRJYTRO4XFo7yuAi/0K9DwjySGjsHB2YrFPnJXo=' \
      -d 'Hello, World!'
    ```
   
    ```json tab="Result"
    +3gnwO0oHXe4kXsr3kegd9jTTqsq3Y6Hm3w26WHR/RM=
    ```

## receiveraw

Receives a raw payload from Orion using the payload key. The payload key is returned by the [sendraw](#sendraw) method

**HTTP Verb**
POST 

**Headers:**
Content-Type: application/octet-stream
c11n-key: Key used to receive the payload

**Request Body**

None

**Returns**

Payload

!!! example
    ```bash tab="curl HTTP request"
    curl -X POST \
      http://127.0.0.1:8888/receiveraw \
      -H 'Content-Type: application/octet-stream' \
      -H 'c11n-key: +3gnwO0oHXe4kXsr3kegd9jTTqsq3Y6Hm3w26WHR/RM='
    ```
   
    ```json tab="Result"
    Hello, World!
    ```

## upcheck

Confirms if Orion is running.

**HTTP Verb**
GET 

**Headers:**
None

**Request Body**
None

**Returns**

*string* : I'm up

!!! example
    ```bash tab="curl HTTP request"
    curl -X GET http://127.0.0.1:8888/upcheck
    ```
   
    ```json tab="Result"
    I'm up
    ```