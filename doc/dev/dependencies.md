# Dependencies

## libsodium

In order to be compatible with the original Haskell Constellation, we used 
[Sodium crypto library](https://download.libsodium.org/doc/) (libsodium) to provide the encryption 
primitives.
 
### Install libsodium

To use this, you will first need to install libsodium on your machine.

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

### Why Lib Sodium?

Why was lib sodium used for the cryptographic primitives (and not bouncy castle)?

The primary reason it was chosen was to provide compatibility with Constellation. Constellation uses the primitives 
provided by lib sodium, in particular the elliptic curve X25519, the XSalsa20 stream cipher, and Poly1305 MAC

While bouncycastle does provide an implementation of the curve (X25519), it does so in the short-Weierstrass form, 
requiring point conversions to interoperate with the lib sodium implementation.

Additionally lib sodium is used to provide an implementation of the Argon2 password hashing function used to secure 
the private keys used with Orion.