# Running Orion

Options for how to run Orion:
* [run Orion with Gradle](#running-orion-with-gradle) 
* build from source and [run the resulting executable](#running-from-distribution-binaries)

## Running orion with Gradle
```
./gradlew run
```
If you want to add runtime options, use `-Pargs`, for example: `./gradlew run -Pargs="-g my-key"`
* see [usage](#usage) for details

## Running from distribution binaries
First [build from source](../development/building.md). Then:
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

## Usage

Usage: `orion [options] [config file]`

### Options 

        -g
        --generatekeys <names>
                generate key pairs for each of the names supplied.
                where <names> are a comma-seperated list
        -h
        --help          print this help message
        -v
        --version       print version information

### Config file
See [configuring Orion](configure.md).


### Generating keys
If you want to generate a pair of public/private keys:
```
orion -g foo
```
This will generate a `foo.key` (private key) and `foo.pub` (public key) in the current folder.
You will be prompted to enter a password to protect the private key. This is optional: however if 
you do enter a password, you need to put this into the [config file](configure.md).

### Upcheck
**To check that Orion is up and running:**
```
$ curl -X GET http://localhost:8080/upcheck
> I'm up!
```