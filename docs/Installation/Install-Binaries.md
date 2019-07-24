description: Install Orion from binary distribution
<!--- END of page meta data -->

# Install Binary Distribution

## Prerequisites

* [Java JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)

!!!important
    Orion is supported on Java 11+. Java 8 support is deprecated and will be removed in a future release.
    
## Install Binaries

Download the Orion [packaged binaries](https://bintray.com/consensys/binaries/orion/_latestVersion#files).

Unpack the downloaded files and change into the `orion-<release>` directory. 

Display Orion command line help to confirm installation: 

```bash tab="Linux/macOS"
$ bin/orion --help
```

```bat tab="Windows"
bin\orion --help
```