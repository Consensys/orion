title: Dependencies
description: Orion dependencies  
<!--- END of page meta data -->

# Dependencies

## libsodium

Orion requires the [Sodium crypto library](https://download.libsodium.org/doc/) (libsodium) to provide the encryption 
primitives.
 
### Install libsodium

#### MacOS

Install using [homebrew](https://brew.sh/):
```bash
brew install libsodium
```

#### Linux

Download the [latest stable version](https://download.libsodium.org/libsodium/releases/LATEST.tar.gz) 
of libsodium.
 
Execute:
``` bash
./configure
make && make check
sudo make install
```

#### Other Systems

Refer to the [libsodium installation docs](https://download.libsodium.org/doc/installation/). 