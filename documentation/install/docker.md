# Docker

There is a provided `Dockerfile` that can be used to build an image of Orion.

To start the container, one needs to provide a volume with a set of keys and a configuration file.

**Example:**

**`orion.conf` configuration file:**
```
nodeurl = "http://127.0.0.1:8080/"
nodeport = 8080
clienturl = "http://127.0.0.1:8888/"
clientport = 8888
workdir = "/data"
publickeys = ["orion.pub"]
privatekeys = ["orion.key"]
tls = "off"
```

**`data` folder:**
```
data
  |--- orion.conf
  |--- orion.pub
  |--- orion.key
```
(you need to use orion to generate the keys)

**Building the image:**
``` 
$ docker build --force-rm -t orion .
``` 
(make sure that you have built the project with `./gradlew build` before building the image)

**Starting the container:**
``` 
$ docker run -d -p 8080:8080 -v data:/data orion
``` 

**To check that orion is up and running:**
```
$ curl -X GET http://localhost:8080/upcheck
> I'm up!
```