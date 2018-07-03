# Running Orion

Running orion with Gradle:
```
./gradlew run
```
If you want to add runtime options, use `-Pargs`, for example: `./gradlew run -Pargs="-g my-key"`

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

## Usage

Usage: orion [options] [config file]

where options include:

        -g
        --generatekeys <names>
                generate key pairs for each of the names supplied.
                where <names> are a comma-seperated list
        -h
        --help          print this help message
        -v
        --version       print version information


### Generating keys
If you want to generate a pair of public/private keys:
```
orion -g foo
```
This will generate a `foo.key` (private key) and `foo.pub` (public key) in the current folder.
