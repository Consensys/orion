# Athena
Athena is the PegaSys component for doing private transactions.

[![Build Status](https://travis-ci.com/ConsenSys/athena.svg?token=2Yxbwhz1bCkWTaWcjCFS&branch=master)](https://travis-ci.com/ConsenSys/athena)

#### Build Instructions

To build `clone` this repo as below, subsequently `cd` into the newly created directory and `clone` the relevant Ethereum test repo. Run with `gradle` like so:

```
git clone https://github.com/ConsenSys/athena
cd athena
gradle build  
```

Perhaps you might like using the ssh protocol to clone... in which case do `git clone git@github.com:ConsenSys/athena.git`

## libsodium

In order to be compatible with the original Haskell Constellation, the lib sodium library has been used to provide the encryption primitives.

In order to use this, you will first need to install lib sodium on your machine.

mac:
`brew install libsodium`

## Native transports

In order to make sure the http related communications are as optimised as possible, we use native transports with the
Java Netty library.  You'll need to build some good things on your OS to help make this sing.

### Linux

```
# RHEL/CentOS/Fedora:
sudo yum install autoconf automake libtool make tar \
                 glibc-devel libaio-devel \
                 libgcc.i686 glibc-devel.i686
# Debian/Ubuntu:
sudo apt-get install autoconf automake libtool make tar \
                     gcc-multilib libaio-dev
```

### Mac OS/BSD

`brew install autoconf automake libtool`

## Running Athena

Kick start the Athena http server after getting everything setup with `gradle run`

## Code coverage report

We use the jacoco test coverage plugin, which will generate coverage data whenever tests are run.

To run the report do:

```gradle test jacocoTestReport```

Then view it at:

```build/reports/jacoco/test/html/index.html```