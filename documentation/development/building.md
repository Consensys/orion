# Checkout code and build it
## Checkout source code

```
git clone git@github.com:ConsenSys/orion.git
```

## Build from source
After you have checked out the code, this will build the distribution binaries.
```
cd orion
./gradlew build  
```

## Run tests
These tasks are run as part of the default Gradle build, but you may want to run them separately.
```
./gradlew test
./gradlew acceptanceTest
```