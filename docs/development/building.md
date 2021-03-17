# Check Out Code and Build it

## Pre-requisites

[libsodium](https://docs.orion.consensys.net/en/latest/HowTo/Dependencies/)

## Check Out Source Code

```
git clone git@github.com:ConsenSys/orion.git
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
