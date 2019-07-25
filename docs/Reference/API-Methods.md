# Client API

The Client API is used by Ethereum clients (for example, Pantheon) to interact with Orion.

The port used by the Client API is defined by the `clientport ` property in the [configuration file](../Configuring-Orion/Configuration-File.md). 
The default port is `8888`.

## createPrivacyGroup

Creates a privacy group with the specified members.

**HTTP Verb**
POST 

**Headers:**
Content-Type: application/json

**Request Body**

`addresses` : *array of strings* - Orion node keys to include in the privacy group

`from` : *string* - Orion node key of node creating the privacy group

`name` : *string* - Name of the privacy group  

`description` : *string* - Description for the privacy group

**Returns**

`privacy group` : *object* - Privacy group object 

!!! example
    ```bash tab="curl HTTP request"
    curl -X POST http://127.0.0.1:8888/createPrivacyGroup \
      -H 'Content-Type: application/json' \
      -d '{ 
        "addresses": [ 
          "g59BmTeJIn7HIcnq8VQWgyh/pDbvbt2eyP0Ii60aDDw=", 
          "negmDcN2P4ODpqn/6WkJ02zT/0w0bjhGpkZ8UP6vARk=" 
        ], 
        "from": "negmDcN2P4ODpqn/6WkJ02zT/0w0bjhGpkZ8UP6vARk=", 
       "name": "Organisation A", 
       "description": "Contains members of Organisation A" 
     }'
    ```
   
    ```json tab="Result"
    {"privacyGroupId":
      "C68ZfeG6wHeXb+CyfwS6NjmmaMWwRaj8ZkrPq/o+S8Q=",
      "name":"Organisation A",
      "description":"Contains members of Organisation A",
      "type":"PANTHEON",
      "members":["g59BmTeJIn7HIcnq8VQWgyh/pDbvbt2eyP0Ii60aDDw=","negmDcN2P4ODpqn/6WkJ02zT/0w0bjhGpkZ8UP6vARk="]
    } 
    ```

## deletePrivacyGroup

Deletes a privacy group.

**HTTP Verb**
POST 

**Headers:**
Content-Type: application/json

**Request Body**

`privacyGroupId` : *string* - ID of the privacy group to delete

`from` : *string* - Orion node key of node deleting the privacy group

**Returns**

`privacyGroupId` : *string* - ID of the deleted privacy group 

!!! example
    ```bash tab="curl HTTP request"
    curl -X POST http://127.0.0.1:8888/deletePrivacyGroup \
      -H 'Content-Type: application/json' \
      -d '{
        "privacyGroupId": "C68ZfeG6wHeXb+CyfwS6NjmmaMWwRaj8ZkrPq/o+S8Q=",
        "from": "negmDcN2P4ODpqn/6WkJ02zT/0w0bjhGpkZ8UP6vARk="
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
    "C68ZfeG6wHeXb+CyfwS6NjmmaMWwRaj8ZkrPq/o+S8Q=" 
    ```

## findPrivacyGroup

Finds all privacy groups containing only the specified members.

**HTTP Verb**
POST 

**Headers:**
Content-Type: application/json

**Request Body**

`addresses` : *array of strings* - Orion node keys for which to return privacy groups

**Returns**

`array of objects` - Privacy group objects for all privacy groups containing only the specified members

!!! example
    ```bash tab="curl HTTP request"
    curl -X POST http://127.0.0.1:8888/findPrivacyGroup \
      -H 'Content-Type: application/json' \
      -d '{
      "addresses" : [
          "g59BmTeJIn7HIcnq8VQWgyh/pDbvbt2eyP0Ii60aDDw=",
          "negmDcN2P4ODpqn/6WkJ02zT/0w0bjhGpkZ8UP6vARk="
      ]
    }'
    ``` 
    
    ```json tab="Result"
    [
      {
        "privacyGroupId": "DVMXn3N6VIerZOJjixFFoGQBu8AleyonJ1sK33aYdtg=",
        "type": "PANTHEON",
        "members": [
          "g59BmTeJIn7HIcnq8VQWgyh/pDbvbt2eyP0Ii60aDDw=",
          "negmDcN2P4ODpqn/6WkJ02zT/0w0bjhGpkZ8UP6vARk="
        ]
      }
    ]
    ``` 

## receive

Receives a payload from Orion using the payload key. The payload key is returned by the [send](#send) method.

**HTTP Verb**
POST 

**Headers:**
Content-Type: application/json

**Request Body**

`key` : *string* - Key used to receive the payload

`to` : *string* - Orion key of the receiver

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
    	"key": "dRQUqPeGy6sj9LQJUYqNlUFroBiWm/tJO+CriTce6AA=",
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

`from` : *string*  - Orion node key of sender

`to` : *array of strings* - Orion node keys to receive this payload 

or
 
 `privacyGroupId` : *string* - Privacy group to receive this payload

**Returns**

`key` : *string* - Key used to receive the payload

!!! example 
    ```bash tab="curl HTTP request with to"
    curl -X POST http://127.0.0.1:8888/send \
      -H 'Content-Type: application/json' \
      -d '{
    	"payload": "SGVsbG8sIFdvcmxkIQ==",
    	"from": "4xanJzyaDPcBVMUSwl/tLp+DbXzd3jF9MKk1yJuyewE=",
    	"to": ["YE5cJRJYTRO4XFo7yuAi/0K9DwjySGjsHB2YrFPnJXo="]
    }'
    ```
    
    ```bash tab="curl HTTP request with privacyGroupId"
    curl -X POST http://127.0.0.1:8888/send \
       -H 'Content-Type: application/json' \
       -d '{
         "payload": "SGVsbG8sIFdvcmxkIQ==",
         "from": "negmDcN2P4ODpqn/6WkJ02zT/0w0bjhGpkZ8UP6vARk=",
         "privacyGroupId": "DVMXn3N6VIerZOJjixFFoGQBu8AleyonJ1sK33aYdtg="
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
c11n-from: Orion node key of the sender
c11n-to: List of Orion node keys to receive this payload

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