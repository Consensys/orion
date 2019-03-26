# Check Out Code and Build it

## Pre-requisites

[libsodium](https://docs.orion.pegasys.tech/en/latest/Installation/Dependencies/)

## Check Out Source Code

```
git clone git@github.com:PegaSysEng/orion.git
```

## Build From Source
Build the distribution binaries:
```
cd orion
./gradlew build  
```

## Run Tests
These tasks are run as part of the default Gradle build but to run them separately:
```
./gradlew test
./gradlew acceptanceTest
```