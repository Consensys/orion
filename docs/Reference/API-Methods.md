# Client API

The Client API is used by Ethereum clients (for example, Pantheon) to interact with Orion.

The port used by the Client API is defined by the `clientport ` property in the [configuration file](../Configuring-Orion/Configuration-File.md). 
The default port is `8888`.

## deletePrivacyGroupId

Deletes a privacy group.

QUESTION: do you have to be a participant in a privacy group to delete it? If yes, what error 
message is returned if you're not? 

**HTTP Verb**
POST 

**Headers:**
Content-Type: application/json

**Request Body**

`privacyGroupId` : *string* - ID of the privacy group to delete

`from` : *string* - Public key of the Orion node deleting the privacy group

**Returns**

`payload` : *string* - Base64 encoded payload QUESTION: what does it return? 

!!! example
    ```bash tab="curl HTTP request"
    curl -X POST http://127.0.0.1:8888/deletePrivacyGroupId \
      -H 'Content-Type: application/json' \
      -d '{
        "privacyGroupId": "DyAOiF/ynpc+JXa2YAGB0bCitSlOMNm+ShmB/7M6C4zKjP39ut+Z7lyQ7YUGRje++UBkaA==",
        "from": "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs="
      }'
    ```
   
    ```json tab="Result"
    ADD THIS 
    ```

## findPrivacyGroupId

Finds all privacy group for the specified members.

QUESTION: if you specify A & B, does it return privacy groups that contain either, or both? If no
privacy groups, what error message is returned?  

**HTTP Verb**
POST 

**Headers:**
Content-Type: application/json

**Request Body**

`addresses` : *array of strings* - Public keys of Orion nodes for which to return privacy groups

**Returns**

`payload` : *string* - Base64 encoded payload QUESTION: what does it return? 

!!! example
    ```bash tab="curl HTTP request"
    curl -X POST http://127.0.0.1:8888/findPrivacyGroupId \
      -H 'Content-Type: application/json' \
      -d '{
      "addresses" : [
          "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=",
          "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs="
      ]
    }'
    ```
   
    ```json tab="Result"
    ADD THIS 
    ```

## privacyGroupId

Creates a privacy group with the specified members.

**HTTP Verb**
POST 

**Headers:**
Content-Type: application/json

**Request Body**

`addresses` : *array of strings* - Public keys of Orion nodes to include in the privacy group

`from` : *string* - Public key of the Orion node creating the privacy group

`name` : *string* - Name of the privacy group QUESTION: is this optional? 

`description` : *string* - Description for the privacy group QUESTION: is this optional?

**Returns**

`payload` : *string* - Base64 encoded payload QUESTION: what does it return? 

!!! example
    ```bash tab="curl HTTP request"
    curl -X POST http://127.0.0.1:8888/privacyGroupId \
      -H 'Content-Type: application/json' \
      -d '{
       "addresses": [
         "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=",
         "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs="
       ],
       "from": "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=",
       "name": "Organisation A",
       "description": "Contains members of Organisation A"
     }'
    ```
   
    ```json tab="Result"
    ADD THIS 
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

### receive with Privacy Group ID 

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

## send

Sends a payload to Orion.

**HTTP Verb**
POST 

**Headers:**
Content-Type: application/json

**Request Body**

`payload` : *string* - Base64 encoded payload

`from` : *string*  - Public key of sender

`to` : *array of strings* - List of public keys to receive this payload


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
   
    ```json tab="Result"
    {"key":"wS+RMprLKIuCaHzOBfPeHmkJWUdOJ7Ji/9U3qj2jbXQ="}
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