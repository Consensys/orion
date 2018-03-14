# Orion
Orion is the PegaSys component for doing private transactions.

[![CircleCI](https://circleci.com/gh/ConsenSys/orion.svg?style=shield&circle-token=5f92fd966a971e60e57f53f2257fe5dda0fcf52c)](https://circleci.com/gh/ConsenSys/orion)

## Building from source
```
git clone git@github.com:ConsenSys/orion.git
cd orion
./gradlew build  
```

## Dependencies

### libsodium

In order to be compatible with the original Haskell Constellation, we used 
[Sodium crypto library](https://download.libsodium.org/doc/) (libsodium) to provide the encryption 
primitives. To use this, you will first need to install libsodium on your machine.

#### Linux
Download the [latest stable version](https://download.libsodium.org/libsodium/releases/LATEST.tar.gz) 
of libsodium tarball and then execute:
```
./configure
make && make check
sudo make install
```

#### MacOS
You can install using [homebrew](https://brew.sh/):
```
brew install libsodium
```

#### Other systems
For more information on how to install libsodium on your system check the 
[libsodium installation docs](https://download.libsodium.org/doc/installation/). 

## Running Orion

Running orion with Gradle:
```
./gradlew run
```
If you want to add runtime options, use `-Pargs`, for example: `gradle run -Pargs="-g my-key"`

Running from distribution binaries (after building from the source):
```
cd build/distributions
tar -xvzf orion*.tar.gz
mv orion*/ orion/
./orion/bin/orion
```

If you want, you can link the binary to your `/usr/local/bin/`
```
ln -s <full_path_to_project_folder>/build/distributions/bin/orion /usr/local/bin/orion
```

e.g. `ln -s /Users/john/git/orion/build/distributions/orion/bin/orion /usr/local/bin/orion`

### Generating keys
If you want to generate a pair of public/private keys:
```
orion -g foo
```
This will generate a `foo.key` (private key) and `foo.pub` (public key) in the current folder.

## Configuring Orion

You can start orion providing a config file:
```
orion foo.conf
```
Where `foo.conf` is a file in the current directory.

### Configuration file

The only required properties are `url` and `port`. Although, it is recommended to set at least the
following properties:

| property name | description |
|---|---|
| url | The URL to advertise to other nodes (reachable by them) |
| port | The local port to listen on |
| workdir | The folder to put stuff in (default: .) |
| othernodes | "Boot nodes" to connect to to discover the network |
| publickeys | Public keys hosted by this node |
| privatekeys | Private keys hosted by this node (in corresponding order) |

Example config file:

```
url = "http://127.0.0.1:9001/"
port = 9001
workdir = "data"
othernodes = ["http://127.0.0.1:9000/"]
publickeys = ["foo.pub"]
privatekeys = ["foo.key"]
```

You can check all the available properties in the  
[`sample.conf`](https://github.com/ConsenSys/orion/blob/master/src/main/resources/sample.conf) file.

## Code coverage

We use the jacoco test coverage plugin, which will generate coverage data whenever tests are run.

To run the report:
```
gradle test jacocoTestReport
```

The report will be available at `build/reports/jacoco/test/html/index.html`

## Feature comparison with Constellation

On [this wiki page](https://github.com/ConsenSys/orion/wiki/Feature-comparison-with-Constellation) 
you can find a breakdown of the features implemented by Orion and the comparison with Constellation's 
features.

## Database disaster recovery

Orion stores all payload information on its internal database. This database is stored on the path 
defined by the `workdir` configuration combined with the path information provided in the `storage` option.

If the database is deleted or corrupted, the node will lose all the payloads stored in its local 
database. It is not possible to recover a lost database without a backup.

[This page](https://github.com/ConsenSys/orion/wiki/Disaster-Recovery-Strategies) contains more 
information about disaster recovery strategies for Orion.